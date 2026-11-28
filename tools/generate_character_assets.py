#!/usr/bin/env python3
"""
Procedural Anatomical Humanoid & Industrial Outfit glTF 2.0 (.glb) Generator
for KineticLab 3D (Phase 4).

Generates multi-LOD rigged binary glTF 2.0 assets:
  - app/src/main/assets/characters/operator_high.glb
  - app/src/main/assets/characters/operator_med.glb
  - app/src/main/assets/characters/operator_low.glb

Includes:
  - 65-bone humanoid skeleton (spine, chest, neck, head, jaw, eyes, clavicles,
    arms, hands, 28 finger phalanges, hips, thighs, knees, calves, ankles, feet, toes)
  - Anatomically sculpted head/face, orbital eyeballs, eyebrows, volumetric hair styles
  - Separate layered industrial clothing (jackets, trousers, 5-finger gloves, work boots)
  - Facial morph targets (blink_L, blink_R, brow_focus, mouth_open)
  - Embedded procedural PBR PNG textures (albedo, normal, metallic-roughness-AO)
  - Embedded glTF keyframe animations (Idle, Walk 4-dir, Jog, Sprint, Jump, Crouch, Interact, Hold, ToolUse, Inspect)
"""

import json
import math
import os
import struct
import zlib

# ==============================================================================
# 1. Vector / Quaternion / Matrix Math Helpers
# ==============================================================================

def v_add(a, b): return (a[0] + b[0], a[1] + b[1], a[2] + b[2])
def v_sub(a, b): return (a[0] - b[0], a[1] - b[1], a[2] - b[2])
def v_mul(a, s): return (a[0] * s, a[1] * s, a[2] * s)
def v_dot(a, b): return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]
def v_cross(a, b):
    return (
        a[1] * b[2] - a[2] * b[1],
        a[2] * b[0] - a[0] * b[2],
        a[0] * b[1] - a[1] * b[0],
    )
def v_len(a): return math.sqrt(v_dot(a, a))
def v_norm(a):
    l = v_len(a)
    return (a[0] / l, a[1] / l, a[2] / l) if l > 1e-8 else (0.0, 1.0, 0.0)

def q_mul(q1, q2):
    x1, y1, z1, w1 = q1
    x2, y2, z2, w2 = q2
    return (
        w1 * x2 + x1 * w2 + y1 * z2 - z1 * y2,
        w1 * y2 - x1 * z2 + y1 * w2 + z1 * x2,
        w1 * z2 + x1 * y2 - y1 * x2 + z1 * w2,
        w1 * w2 - x1 * x2 - y1 * y2 - z1 * z2,
    )

def q_axis_angle(axis, rad):
    ax = v_norm(axis)
    h = rad * 0.5
    s = math.sin(h)
    return (ax[0] * s, ax[1] * s, ax[2] * s, math.cos(h))

def q_euler_xyz(rx, ry, rz):
    qx = q_axis_angle((1.0, 0.0, 0.0), rx)
    qy = q_axis_angle((0.0, 1.0, 0.0), ry)
    qz = q_axis_angle((0.0, 0.0, 1.0), rz)
    return q_mul(qy, q_mul(qx, qz))

def mat4_translate(t):
    # Column-major 4x4 matrix
    return [
        1.0, 0.0, 0.0, 0.0,
        0.0, 1.0, 0.0, 0.0,
        0.0, 0.0, 1.0, 0.0,
        t[0], t[1], t[2], 1.0
    ]

# ==============================================================================
# 2. Skeleton Definition (65 Bones, Y-up, Facing -Z, Total Height ~1.78m)
# ==============================================================================

# All positions in World Bind Space (meters). Feet sole sits at Y=0.0.
# Left is -X, Right is +X, Forward is -Z, Back is +Z.
BONES_SPEC = [
    # Core spine
    ("Root",            None,            (0.000, 0.000,  0.000)), # 0
    ("Pelvis",          "Root",          (0.000, 0.940,  0.000)), # 1
    ("Spine",           "Pelvis",        (0.000, 1.045,  0.004)), # 2
    ("Spine_01",        "Spine",         (0.000, 1.155,  0.008)), # 3
    ("Spine_02",        "Spine_01",      (0.000, 1.265,  0.005)), # 4
    ("Chest",           "Spine_02",      (0.000, 1.385, -0.002)), # 5
    ("Neck",            "Chest",         (0.000, 1.495,  0.000)), # 6
    ("Head",            "Neck",          (0.000, 1.595, -0.008)), # 7
    # Face & Eyes
    ("Jaw",             "Head",          (0.000, 1.612, -0.048)), # 8
    ("LeftEye",         "Head",          (-0.032, 1.678, -0.072)), # 9
    ("RightEye",        "Head",          ( 0.032, 1.678, -0.072)), # 10
    # Left Arm (-X)
    ("LeftShoulder",    "Chest",         (-0.068, 1.445,  0.005)), # 11
    ("LeftUpperArm",    "LeftShoulder",  (-0.195, 1.430,  0.012)), # 12
    ("LeftElbow",       "LeftUpperArm",  (-0.345, 1.295,  0.022)), # 13
    ("LeftForearm",     "LeftElbow",     (-0.425, 1.220,  0.018)), # 14
    ("LeftWrist",       "LeftForearm",   (-0.525, 1.125,  0.006)), # 15
    ("LeftHand",        "LeftWrist",     (-0.555, 1.095,  0.002)), # 16
]

def _add_finger_bones(side_prefix, sign_x, hand_name):
    hx, hy, hz = (-0.555 * (-1 if sign_x > 0 else 1), 1.095, 0.002)
    # Direction along arm bind pose: roughly diag down-out: (-0.70*sign_x, -0.71, 0)
    # Wait: Left arm has sign_x = -1, Right arm has sign_x = +1
    hx = 0.555 * sign_x
    hy = 1.095
    hz = 0.002
    dx, dy = 0.707 * sign_x, -0.707
    # Thumb angles slightly forward (-Z) and out
    out = []
    out.append((f"{side_prefix}Thumb1", hand_name, (hx + 0.022*dx, hy + 0.022*dy - 0.006, hz - 0.028)))
    out.append((f"{side_prefix}Thumb2", f"{side_prefix}Thumb1", (hx + 0.046*dx, hy + 0.046*dy - 0.010, hz - 0.046)))

    fingers = [
        ("Index",  -0.028, 0.068, 0.028, 0.020),
        ("Middle", -0.009, 0.072, 0.030, 0.022),
        ("Ring",    0.010, 0.069, 0.028, 0.020),
        ("Little",  0.028, 0.064, 0.023, 0.017),
    ]
    for fname, zoff, l1, l2, l3 in fingers:
        p1 = (hx + l1*dx, hy + l1*dy, hz + zoff)
        p2 = (p1[0] + l2*dx, p1[1] + l2*dy, hz + zoff)
        p3 = (p2[0] + l3*dx, p2[1] + l3*dy, hz + zoff)
        out.append((f"{side_prefix}{fname}1", hand_name, p1))
        out.append((f"{side_prefix}{fname}2", f"{side_prefix}{fname}1", p2))
        out.append((f"{side_prefix}{fname}3", f"{side_prefix}{fname}2", p3))
    return out

BONES_SPEC.extend(_add_finger_bones("Left", -1.0, "LeftHand"))   # 17..30 (14 bones)
BONES_SPEC.extend([
    ("RightShoulder",   "Chest",         ( 0.068, 1.445,  0.005)), # 31
    ("RightUpperArm",   "RightShoulder", ( 0.195, 1.430,  0.012)), # 32
    ("RightElbow",      "RightUpperArm", ( 0.345, 1.295,  0.022)), # 33
    ("RightForearm",    "RightElbow",    ( 0.425, 1.220,  0.018)), # 34
    ("RightWrist",      "RightForearm",  ( 0.525, 1.125,  0.006)), # 35
    ("RightHand",       "RightWrist",    ( 0.555, 1.095,  0.002)), # 36
])
BONES_SPEC.extend(_add_finger_bones("Right", 1.0, "RightHand"))   # 37..50 (14 bones)
BONES_SPEC.extend([
    # Left Leg (-X)
    ("LeftHip",         "Pelvis",        (-0.098, 0.905,  0.000)), # 51
    ("LeftThigh",       "LeftHip",       (-0.102, 0.855,  0.002)), # 52
    ("LeftKnee",        "LeftThigh",     (-0.098, 0.485, -0.008)), # 53
    ("LeftCalf",        "LeftKnee",      (-0.098, 0.365,  0.012)), # 54
    ("LeftAnkle",       "LeftCalf",      (-0.098, 0.088,  0.014)), # 55
    ("LeftFoot",        "LeftAnkle",     (-0.098, 0.042, -0.035)), # 56
    ("LeftToes",        "LeftFoot",      (-0.098, 0.022, -0.115)), # 57
    # Right Leg (+X)
    ("RightHip",        "Pelvis",        ( 0.098, 0.905,  0.000)), # 58
    ("RightThigh",      "RightHip",      ( 0.102, 0.855,  0.002)), # 59
    ("RightKnee",       "RightThigh",    ( 0.098, 0.485, -0.008)), # 60
    ("RightCalf",       "RightKnee",     ( 0.098, 0.365,  0.012)), # 61
    ("RightAnkle",      "RightCalf",     ( 0.098, 0.088,  0.014)), # 62
    ("RightFoot",       "RightAnkle",    ( 0.098, 0.042, -0.035)), # 63
    ("RightToes",       "RightFoot",     ( 0.098, 0.022, -0.115)), # 64
])

BONE_INDEX = {b[0]: i for i, b in enumerate(BONES_SPEC)}
assert len(BONES_SPEC) == 65

# ==============================================================================
# 3. Skin Weighting Utilities
# ==============================================================================

def _seg_closest_t(p, a, b):
    ab = v_sub(b, a)
    denom = v_dot(ab, ab)
    if denom < 1e-8:
        return 0.0
    t = v_dot(v_sub(p, a), ab) / denom
    return max(0.0, min(1.0, t))

def compute_capsule_skin_weights(pos, candidate_segments):
    """
    Computes smooth linear blend weights over candidate bone segments [(bone_idx, pos_a, pos_b, radius, bias), ...].
    Returns (joints4, weights4) normalized to sum = 1.0.
    """
    scores = {}
    for b_idx, a, b, falloff, bias in candidate_segments:
        t = _seg_closest_t(pos, a, b)
        cp = v_add(a, v_mul(v_sub(b, a), t))
        d = v_len(v_sub(pos, cp))
        w = math.exp(-(d / max(falloff, 1e-4)) ** 2 * 2.2) * bias
        if b_idx in scores:
            scores[b_idx] = max(scores[b_idx], w)
        else:
            scores[b_idx] = w
    items = sorted(scores.items(), key=lambda x: x[1], reverse=True)[:4]
    while len(items) < 4:
        items.append((0, 0.0))
    total = sum(w for _, w in items)
    if total < 1e-8:
        return (items[0][0], 0, 0, 0), (1.0, 0.0, 0.0, 0.0)
    joints = tuple(j for j, _ in items)
    weights = tuple(w / total for _, w in items)
    return joints, weights

# ==============================================================================
# 4. Sculpted Anatomical Mesh Builders
# ==============================================================================

class PrimitiveBuilder:
    def __init__(self, name, material_idx):
        self.name = name
        self.material_idx = material_idx
        self.positions = []
        self.normals = []
        self.tangents = []
        self.uvs = []
        self.joints = []
        self.weights = []
        self.indices = []
        self.morph_targets = {}  # target_name -> list of delta_pos (same len as positions)

    def ensure_morph(self, morph_name):
        if morph_name not in self.morph_targets:
            self.morph_targets[morph_name] = [(0.0, 0.0, 0.0)] * len(self.positions)

    def add_vertex(self, pos, normal, uv, joints, weights, morph_deltas=None):
        idx = len(self.positions)
        self.positions.append(pos)
        self.normals.append(v_norm(normal))
        self.tangents.append((1.0, 0.0, 0.0, 1.0))  # recomputed smoothly from UVs below
        self.uvs.append(uv)
        self.joints.append(joints)
        self.weights.append(weights)
        for mname, arr in self.morph_targets.items():
            arr.append(morph_deltas.get(mname, (0.0, 0.0, 0.0)) if morph_deltas else (0.0, 0.0, 0.0))
        return idx

    def add_tri(self, i0, i1, i2):
        self.indices.extend((i0, i1, i2))

    def add_quad(self, i0, i1, i2, i3):
        self.indices.extend((i0, i1, i2, i0, i2, i3))

    def recompute_normals_and_tangents(self, smooth=True):
        n_count = len(self.positions)
        if smooth:
            acc_n = [[0.0, 0.0, 0.0] for _ in range(n_count)]
        else:
            acc_n = None
        acc_t = [[0.0, 0.0, 0.0] for _ in range(n_count)]

        for k in range(0, len(self.indices), 3):
            i0, i1, i2 = self.indices[k], self.indices[k + 1], self.indices[k + 2]
            p0, p1, p2 = self.positions[i0], self.positions[i1], self.positions[i2]
            uv0, uv1, uv2 = self.uvs[i0], self.uvs[i1], self.uvs[i2]
            e1 = v_sub(p1, p0)
            e2 = v_sub(p2, p0)
            fn = v_cross(e1, e2)
            if smooth:
                for vi in (i0, i1, i2):
                    acc_n[vi][0] += fn[0]
                    acc_n[vi][1] += fn[1]
                    acc_n[vi][2] += fn[2]

            du1 = uv1[0] - uv0[0]
            dv1 = uv1[1] - uv0[1]
            du2 = uv2[0] - uv0[0]
            dv2 = uv2[1] - uv0[1]
            det = du1 * dv2 - du2 * dv1
            if abs(det) > 1e-8:
                inv_d = 1.0 / det
                tx = (e1[0] * dv2 - e2[0] * dv1) * inv_d
                ty = (e1[1] * dv2 - e2[1] * dv1) * inv_d
                tz = (e1[2] * dv2 - e2[2] * dv1) * inv_d
            else:
                tx, ty, tz = 1.0, 0.0, 0.0
            for vi in (i0, i1, i2):
                acc_t[vi][0] += tx
                acc_t[vi][1] += ty
                acc_t[vi][2] += tz

        for vi in range(n_count):
            if smooth and v_len(acc_n[vi]) > 1e-7:
                n = v_norm(tuple(acc_n[vi]))
                self.normals[vi] = n
            else:
                n = self.normals[vi]
            t = tuple(acc_t[vi])
            # Gram-Schmidt orthogonalize
            tdn = v_dot(t, n)
            t_ortho = v_sub(t, v_mul(n, tdn))
            if v_len(t_ortho) < 1e-6:
                up = (0.0, 1.0, 0.0) if abs(n[1]) < 0.9 else (1.0, 0.0, 0.0)
                t_ortho = v_cross(up, n)
            t_ortho = v_norm(t_ortho)
            self.tangents[vi] = (t_ortho[0], t_ortho[1], t_ortho[2], 1.0)

def _loft_rings(builder, rings, u_steps, closed_v=False, weight_fn=None, morph_fn=None):
    """
    Lofts a list of cross-section rings into clean triangle mesh.
    Each ring item is a tuple (y_or_center, ring_pts_list, v_coord).
    """
    ring_verts = []
    n_rings = len(rings)
    for ri, (pts, v_uv) in enumerate(rings):
        row = []
        for ui in range(u_steps + 1):
            p = pts[ui % u_steps]
            u_uv = ui / float(u_steps)
            j, w = weight_fn(p, ri, ui)
            md = morph_fn(p, ri, ui) if morph_fn else None
            # Temporary normal pointing outward; recomputed smoothly later
            idx = builder.add_vertex(p, (0.0, 0.0, -1.0), (u_uv, v_uv), j, w, md)
            row.append(idx)
        ring_verts.append(row)

    for ri in range(n_rings - 1):
        r0 = ring_verts[ri]
        r1 = ring_verts[ri + 1]
        for ui in range(u_steps):
            # Outward winding (CCW seen from outside)
            builder.add_quad(r0[ui], r1[ui], r1[ui + 1], r0[ui + 1])

    if closed_v and n_rings >= 2:
        # Cap bottom and top
        pts0 = rings[0][0]
        c0 = (sum(p[0] for p in pts0)/len(pts0), sum(p[1] for p in pts0)/len(pts0), sum(p[2] for p in pts0)/len(pts0))
        j0, w0 = weight_fn(c0, 0, 0)
        c0_idx = builder.add_vertex(c0, (0.0, -1.0, 0.0), (0.5, 0.0), j0, w0)
        r0 = ring_verts[0]
        for ui in range(u_steps):
            builder.add_tri(c0_idx, r0[ui + 1], r0[ui])

        pts1 = rings[-1][0]
        c1 = (sum(p[0] for p in pts1)/len(pts1), sum(p[1] for p in pts1)/len(pts1), sum(p[2] for p in pts1)/len(pts1))
        j1, w1 = weight_fn(c1, n_rings - 1, 0)
        c1_idx = builder.add_vertex(c1, (0.0, 1.0, 0.0), (0.5, 1.0), j1, w1)
        r1 = ring_verts[-1]
        for ui in range(u_steps):
            builder.add_tri(c1_idx, r1[ui], r1[ui + 1])

def _superellipse_ring(cx, cy, cz, rx, rz, n_pts, front_bulge=0.0, back_bulge=0.0, sq=2.0, twist=0.0):
    pts = []
    for i in range(n_pts):
        ang = (i / float(n_pts)) * 2.0 * math.pi + twist
        ca, sa = math.cos(ang), math.sin(ang)
        # Forward is -Z (sa < 0 when ang ~ 3*pi/2, let's make ang=0 -> +X, ang=pi/2 -> +Z (back), ang=3pi/2 -> -Z (front))
        sx = math.copysign(abs(ca) ** (2.0 / sq), ca)
        sz = math.copysign(abs(sa) ** (2.0 / sq), sa)
        z_mod = rz
        if sa < 0.0:
            z_mod += front_bulge * ((-sa) ** 1.4)
        else:
            z_mod += back_bulge * (sa ** 1.4)
        pts.append((cx + sx * rx, cy, cz + sz * z_mod))
    return pts

def _tube_along_axis(center, axis_dir, up_hint, rx, ry, length_a, length_b, u_steps, v_steps,
                     profile_fn, weight_fn, builder, cap_start=True, cap_end=True):
    ax = v_norm(axis_dir)
    side = v_norm(v_cross(up_hint, ax))
    up = v_norm(v_cross(ax, side))
    rings = []
    for vi in range(v_steps + 1):
        t = vi / float(v_steps)
        s = length_a + (length_b - length_a) * t
        c = v_add(center, v_mul(ax, s))
        sx, sy, off_u, off_s = profile_fn(t)
        c = v_add(c, v_add(v_mul(up, off_u), v_mul(side, off_s)))
        ring = []
        for ui in range(u_steps):
            ang = (ui / float(u_steps)) * 2.0 * math.pi
            pt = v_add(c, v_add(v_mul(side, math.cos(ang) * rx * sx), v_mul(up, math.sin(ang) * ry * sy)))
            ring.append(pt)
        rings.append((ring, t))
    _loft_rings(builder, rings, u_steps, closed_v=(cap_start and cap_end), weight_fn=weight_fn)

# ==============================================================================
# 5. Character Anatomy & Clothing Builders per LOD
# ==============================================================================

def _build_head_face(builder, u_steps, v_steps):
    """
    Sculpted anatomical human head + neck + ears + nose + lips + eye sockets + jaw,
    with real morph targets: blink_L, blink_R, brow_focus, mouth_open.
    Head bone ~ (0, 1.595, -0.008), cranial crown at Y=1.78, chin at Y=1.555.
    """
    builder.ensure_morph("blink_L")
    builder.ensure_morph("blink_R")
    builder.ensure_morph("brow_focus")
    builder.ensure_morph("mouth_open")

    neck_idx = BONE_INDEX["Neck"]
    head_idx = BONE_INDEX["Head"]
    jaw_idx = BONE_INDEX["Jaw"]
    chest_idx = BONE_INDEX["Chest"]

    rings = []
    # Vertical slices from upper-chest under-collar base Y=1.35 to crown Y=1.78
    for vi in range(v_steps + 1):
        t = vi / float(v_steps)
        y = 1.35 + 0.43 * t
        ring = []
        for ui in range(u_steps):
            # ang=0 -> +X, pi/2 -> +Z (back of skull), pi -> -X, 3pi/2 -> -Z (face front!)
            ang = (ui / float(u_steps)) * 2.0 * math.pi
            ca = math.cos(ang)
            sa = math.sin(ang)
            face_front = max(0.0, -sa)  # 1 at center of face (-Z)

            if t < 0.19:
                # Upper chest / trapezius / clavicle base tucked solidly inside jacket collar
                ct = t / 0.19
                rx = 0.140 * (1.0 - ct) + 0.068 * ct
                rz = 0.098 * (1.0 - ct) + 0.066 * ct
                cz = 0.008 * (1.0 - ct) + 0.004 * ct
                cy = y
            elif t < 0.37:
                # Neck + Sternocleidomastoid taper
                nt = (t - 0.19) / 0.18
                rx = 0.068 * (1.0 - 0.14 * nt) + 0.004 * math.sin(nt * math.pi)
                rz = 0.066 * (1.0 - 0.12 * nt)
                cz = 0.004 - 0.012 * nt
                cy = y
            else:
                # Head ellipsoid from chin (1.51) to crown (1.78)
                ht = (t - 0.37) / 0.63  # 0 at chin base, 1 at crown
                base_rx = 0.081 * math.sin(max(0.04, min(0.985, ht * 0.92 + 0.06)) * math.pi) ** 0.78
                base_rz = 0.098 * math.sin(max(0.04, min(0.985, ht * 0.92 + 0.06)) * math.pi) ** 0.75
                # Occipital back cranium fullness + narrower jaw
                jaw_narrow = 0.68 + 0.32 * min(1.0, ht / 0.42)
                rx = max(0.008, base_rx * (jaw_narrow if face_front > 0.1 else (0.85 + 0.15 * ht)))
                rz = max(0.008, base_rz)
                cz = -0.014 + 0.016 * (1.0 - ht) * 0.2
                cy = y

                # Facial anatomical sculpting (-Z hemisphere)
                if face_front > 0.05:
                    fx = ca  # -1 left, +1 right
                    # Zygomatic cheekbones & temple taper
                    cheek = math.exp(-((ht - 0.45) / 0.10) ** 2) * math.exp(-((abs(fx) - 0.45) / 0.22) ** 2)
                    jaw_corner = math.exp(-((ht - 0.23) / 0.08) ** 2) * math.exp(-((abs(fx) - 0.62) / 0.16) ** 2)
                    # Supraorbital brow ridge
                    brow = math.exp(-((ht - 0.635) / 0.058) ** 2) * math.exp(-(fx / 0.56) ** 2)
                    # Orbital eye sockets recessed ht ~ 0.56, |fx| ~ 0.34
                    socket_l = math.exp(-((ht - 0.56) / 0.060) ** 2) * math.exp(-((fx + 0.34) / 0.15) ** 2)
                    socket_r = math.exp(-((ht - 0.56) / 0.060) ** 2) * math.exp(-((fx - 0.34) / 0.15) ** 2)
                    socket = socket_l + socket_r
                    # Nasal bridge, tip & nostril wings
                    nose_tip = math.exp(-((ht - 0.415) / 0.062) ** 2) * math.exp(-(fx / 0.105) ** 2)
                    nose_bridge = math.exp(-((ht - 0.515) / 0.085) ** 2) * math.exp(-(fx / 0.075) ** 2)
                    nostril_wing = math.exp(-((ht - 0.39) / 0.032) ** 2) * math.exp(-((abs(fx) - 0.13) / 0.055) ** 2)
                    # Lips & philtrum crease
                    upper_lip = math.exp(-((ht - 0.318) / 0.022) ** 2) * math.exp(-(fx / 0.20) ** 2)
                    lower_lip = math.exp(-((ht - 0.272) / 0.024) ** 2) * math.exp(-(fx / 0.17) ** 2)
                    mouth_seam = math.exp(-((ht - 0.295) / 0.011) ** 2) * math.exp(-(fx / 0.21) ** 2)
                    # Chin prominence
                    chin = math.exp(-((ht - 0.145) / 0.068) ** 2) * math.exp(-(fx / 0.22) ** 2)

                    z_push = (
                        0.0125 * brow +
                        0.0115 * cheek +
                        0.0055 * jaw_corner -
                        0.0175 * socket +
                        0.0245 * nose_tip +
                        0.0155 * nose_bridge +
                        0.0085 * nostril_wing +
                        0.0090 * upper_lip +
                        0.0080 * lower_lip -
                        0.0060 * mouth_seam +
                        0.0135 * chin
                    ) * face_front
                    rz += z_push
                    rx += 0.004 * jaw_corner

                # Detailed pinna ears at |fx| > 0.88, ht ~ 0.45..0.61
                ear_mask = math.exp(-((abs(ca) - 0.96) / 0.11) ** 2) * math.exp(-((ht - 0.50) / 0.082) ** 2)
                rx += 0.0135 * ear_mask

            pt = (ca * rx, cy, cz + sa * rz)
            ring.append(pt)
        rings.append((ring, t))

    def head_weight(p, ri, ui):
        y = p[1]
        if y < 1.43:
            return (chest_idx, neck_idx, 0, 0), (0.88, 0.12, 0.0, 0.0)
        elif y < 1.515:
            wt = max(0.0, min(1.0, (y - 1.43) / 0.085))
            return (neck_idx, chest_idx, 0, 0), (wt, 1.0 - wt, 0.0, 0.0)
        elif y < 1.595:
            wt = max(0.0, min(1.0, (y - 1.515) / 0.08))
            return (head_idx, neck_idx, 0, 0), (wt, 1.0 - wt, 0.0, 0.0)
        elif p[2] < -0.025 and y < 1.655:
            jaw_w = max(0.0, min(0.65, (1.655 - y) / 0.065 * 0.65))
            return (head_idx, jaw_idx, 0, 0), (1.0 - jaw_w, jaw_w, 0.0, 0.0)
        return (head_idx, 0, 0, 0), (1.0, 0.0, 0.0, 0.0)

    def head_morph(p, ri, ui):
        deltas = {}
        x, y, z = p
        if z < -0.045:
            # Left eye blink around (-0.032, 1.682)
            dl = math.exp(-((x + 0.032) / 0.016) ** 2 - ((y - 1.688) / 0.012) ** 2)
            dr = math.exp(-((x - 0.032) / 0.016) ** 2 - ((y - 1.688) / 0.012) ** 2)
            if dl > 0.05:
                deltas["blink_L"] = (0.0, -0.007 * dl, -0.002 * dl)
            if dr > 0.05:
                deltas["blink_R"] = (0.0, -0.007 * dr, -0.002 * dr)
            db = math.exp(-((y - 1.702) / 0.015) ** 2 - (x / 0.050) ** 2)
            if db > 0.05:
                deltas["brow_focus"] = (0.0, -0.004 * db, -0.002 * db)
            dm = math.exp(-((y - 1.612) / 0.014) ** 2 - (x / 0.025) ** 2)
            if dm > 0.05:
                deltas["mouth_open"] = (0.0, -0.008 * dm, 0.001 * dm)
        return deltas

    _loft_rings(builder, rings, u_steps, closed_v=True, weight_fn=head_weight, morph_fn=head_morph)

def _build_eyes(builder, segs):
    """Left and Right anatomical eyeballs weighted 100% to LeftEye and RightEye."""
    for side_name, cx in [("LeftEye", -0.032), ("RightEye", 0.032)]:
        b_idx = BONE_INDEX[side_name]
        center = (cx, 1.678, -0.074)
        rad = 0.0135
        rings = []
        for vi in range(segs + 1):
            # vi=0 -> front pole (-Z), vi=segs -> back pole (+Z) so UVs put iris dead-center
            phi = (vi / float(segs)) * math.pi
            sp, cp = math.sin(phi), math.cos(phi)
            ring = []
            for ui in range(segs * 2):
                th = (ui / float(segs * 2)) * 2.0 * math.pi
                pt = (
                    center[0] + math.cos(th) * sp * rad,
                    center[1] + math.sin(th) * sp * rad,
                    center[2] - cp * rad
                )
                ring.append(pt)
            rings.append((ring, vi / float(segs)))
        # Custom sphere loft with radial UV centered at (0.5, 0.5) for iris texture
        ring_verts = []
        for vi, (pts, t_v) in enumerate(rings):
            phi = t_v * math.pi
            uv_r = 0.45 * (t_v)
            row = []
            for ui in range(segs * 2 + 1):
                p = pts[ui % (segs * 2)]
                th = (ui / float(segs * 2)) * 2.0 * math.pi
                u = 0.5 + math.cos(th) * uv_r
                v = 0.5 + math.sin(th) * uv_r
                idx = builder.add_vertex(p, v_sub(p, center), (u, v), (b_idx, 0, 0, 0), (1.0, 0.0, 0.0, 0.0))
                row.append(idx)
            ring_verts.append(row)
        for vi in range(segs):
            r0, r1 = ring_verts[vi], ring_verts[vi + 1]
            for ui in range(segs * 2):
                builder.add_quad(r0[ui], r0[ui + 1], r1[ui + 1], r1[ui])

def _build_eyebrows(builder, segs):
    head_idx = BONE_INDEX["Head"]
    builder.ensure_morph("brow_focus")
    for sign in (-1.0, 1.0):
        rings = []
        for vi in range(5):
            t = vi / 4.0
            cx = sign * (0.015 + 0.036 * t)
            cy = 1.698 + 0.006 * math.sin(t * math.pi) - 0.002 * t
            cz = -0.088 + 0.012 * (t ** 1.6)
            rad_y = 0.0036 * (0.65 + 0.45 * math.sin(t * math.pi))
            rad_z = 0.0035
            ring = []
            for ui in range(segs):
                a = (ui / float(segs)) * 2.0 * math.pi
                ring.append((cx, cy + math.sin(a) * rad_y, cz + math.cos(a) * rad_z))
            rings.append((ring, t))
        _loft_rings(
            builder, rings, segs, closed_v=True,
            weight_fn=lambda p, r, u: ((head_idx, 0, 0, 0), (1.0, 0.0, 0.0, 0.0)),
            morph_fn=lambda p, r, u: {"brow_focus": (0.0, -0.004, -0.002)}
        )

def _build_hairstyle(builder, style_name, u_steps, v_steps):
    head_idx = BONE_INDEX["Head"]
    neck_idx = BONE_INDEX["Neck"]
    rings = []

    if style_name == "Hair_CrewCut":
        y0, y1 = 1.685, 1.792
        rx_scale, rz_scale = 1.03, 1.03
        part_shift = 0.0
    elif style_name == "Hair_SidePart":
        y0, y1 = 1.675, 1.805
        rx_scale, rz_scale = 1.08, 1.08
        part_shift = 0.009
    elif style_name == "Hair_TiedBack":
        y0, y1 = 1.668, 1.800
        rx_scale, rz_scale = 1.07, 1.07
        part_shift = 0.0
    else:  # Hair_TechCap (peaked cap shell)
        y0, y1 = 1.698, 1.805
        rx_scale, rz_scale = 1.09, 1.11
        part_shift = 0.0

    for vi in range(v_steps + 1):
        t = vi / float(v_steps)
        y = y0 + (y1 - y0) * t
        ht = (y - 1.538) / 0.242
        base_rx = 0.083 * rx_scale * math.sin(max(0.08, min(0.99, ht * 0.92 + 0.06)) * math.pi) ** 0.72
        base_rz = 0.101 * rz_scale * math.sin(max(0.08, min(0.99, ht * 0.92 + 0.06)) * math.pi) ** 0.72
        if t > 0.95:
            base_rx *= max(0.08, (1.0 - t) * 20.0)
            base_rz *= max(0.08, (1.0 - t) * 20.0)
        ring = []
        for ui in range(u_steps):
            ang = (ui / float(u_steps)) * 2.0 * math.pi
            ca, sa = math.cos(ang), math.sin(ang)
            # Push hairline back on forehead (-Z) so face remains open!
            forehead = max(0.0, -sa)
            z_off = -0.012
            if forehead > 0.2 and t < 0.45:
                # Raise front hairline subtly on temples/forehead
                y_local = y + 0.018 * forehead * (1.0 - t * 1.5)
            else:
                y_local = y
            # Volumetric strand ridges
            ridge = 1.0 + 0.025 * math.sin(ang * 14.0 + t * 5.0)
            # Side-part asymmetrical crest
            crest = part_shift * max(0.0, ca + 0.3) * math.sin(t * math.pi)
            if style_name == "Hair_TechCap" and forehead > 0.35 and t < 0.28:
                # Cap visor brim extending forward (-Z)
                brim = (0.048 * (1.0 - t / 0.28)) * (forehead ** 1.2)
            else:
                brim = 0.0
            ring.append((ca * base_rx * ridge, y_local + crest, z_off + sa * base_rz * ridge - brim))
        rings.append((ring, t))

    _loft_rings(
        builder, rings, u_steps, closed_v=True,
        weight_fn=lambda p, r, u: ((head_idx, 0, 0, 0), (1.0, 0.0, 0.0, 0.0))
    )

    if style_name == "Hair_TiedBack":
        # Sculpted tied-back low bun volume at back of skull (+Z)
        _tube_along_axis(
            center=(0.0, 1.690, 0.098), axis_dir=(0.0, -0.35, 1.0), up_hint=(0.0, 1.0, 0.0),
            rx=0.032, ry=0.030, length_a=-0.01, length_b=0.055,
            u_steps=max(8, u_steps // 2), v_steps=5,
            profile_fn=lambda t: (math.sin(max(0.15, t * 0.9 + 0.05) * math.pi), math.sin(max(0.15, t * 0.9 + 0.05) * math.pi), 0.0, 0.0),
            weight_fn=lambda p, r, u: ((head_idx, neck_idx, 0, 0), (0.92, 0.08, 0.0, 0.0)),
            builder=builder
        )

def _build_hand_and_fingers(builder, side_prefix, sign_x, u_palm, v_palm, u_finger, is_gloved=False):
    """
    Sculpts anatomical forearm-wrist bridge + palm + 5 articulated fingers per hand,
    eliminating any seam at the wrist cuff during 1st-person or 3rd-person bending.
    """
    elbow_idx = BONE_INDEX[f"{side_prefix}Elbow"]
    forearm_idx = BONE_INDEX[f"{side_prefix}Forearm"]
    wrist_idx = BONE_INDEX[f"{side_prefix}Wrist"]
    hand_idx = BONE_INDEX[f"{side_prefix}Hand"]
    elbow_pos = BONES_SPEC[elbow_idx][2]
    forearm_pos = BONES_SPEC[forearm_idx][2]
    wrist_pos = BONES_SPEC[wrist_idx][2]
    hand_pos = BONES_SPEC[hand_idx][2]
    axis = v_norm(v_sub(hand_pos, wrist_pos))  # diag down-out along arm

    # Continuous forearm -> wrist bridge tucked smoothly inside jacket/glove cuff (vital for 1st-person sleeves!)
    bridge_segs = [
        (elbow_idx,   elbow_pos,   forearm_pos, 0.068, 0.8),
        (forearm_idx, forearm_pos, wrist_pos,   0.060, 1.0),
        (wrist_idx,   wrist_pos,   hand_pos,    0.055, 1.0),
    ]
    wrist_len = v_len(v_sub(wrist_pos, elbow_pos))
    bridge_start = -wrist_len * (0.28 if is_gloved else 0.92)
    bridge_rad = 0.041 if is_gloved else 0.036
    _tube_along_axis(
        center=wrist_pos, axis_dir=axis, up_hint=(0.0, 0.0, 1.0),
        rx=bridge_rad, ry=bridge_rad * 0.92, length_a=bridge_start, length_b=0.008,
        u_steps=u_palm, v_steps=max(4, v_palm),
        profile_fn=lambda t: (
            (1.18 - 0.22 * t) * (1.08 if is_gloved and t < 0.25 else 1.0),
            (1.14 - 0.22 * t) * (1.08 if is_gloved and t < 0.25 else 1.0),
            0.0, 0.0
        ),
        weight_fn=lambda p, r, u, sd=bridge_segs: compute_capsule_skin_weights(p, sd),
        builder=builder
    )

    # Palm sculpted flattened box-oval overlapping Wrist -> Knuckles
    palm_segs = [
        (forearm_idx, forearm_pos, wrist_pos, 0.055, 0.35),
        (wrist_idx,   wrist_pos,   hand_pos,  0.055, 0.70),
        (hand_idx,    wrist_pos,   hand_pos,  0.058, 1.00),
    ]
    _tube_along_axis(
        center=wrist_pos, axis_dir=axis, up_hint=(0.0, 0.0, 1.0),
        rx=0.046 if is_gloved else 0.043, ry=0.019 if is_gloved else 0.017,
        length_a=-0.018, length_b=0.098,
        u_steps=u_palm, v_steps=v_palm,
        profile_fn=lambda t: (
            0.74 + 0.34 * math.sin(t * math.pi * 0.85),
            0.96 - 0.16 * t,
            0.0, 0.0
        ),
        weight_fn=lambda p, r, u, sd=palm_segs: compute_capsule_skin_weights(p, sd),
        builder=builder
    )

    # 5 Articulated Fingers (Thumb + Index/Middle/Ring/Little) overlapping deep into palm
    fingers = [
        ("Thumb",  2, 0.0128 if is_gloved else 0.0118, 0.023),
        ("Index",  3, 0.0108 if is_gloved else 0.0098, 0.024),
        ("Middle", 3, 0.0112 if is_gloved else 0.0103, 0.026),
        ("Ring",   3, 0.0105 if is_gloved else 0.0096, 0.024),
        ("Little", 3, 0.0092 if is_gloved else 0.0084, 0.020),
    ]
    for fname, phalanges, rad, seg_len in fingers:
        chain = [BONE_INDEX[f"{side_prefix}{fname}{k}"] for k in range(1, phalanges + 1)]
        p_start = BONES_SPEC[chain[0]][2]
        p_end = BONES_SPEC[chain[-1]][2]
        f_dir = v_norm(v_sub(p_end, p_start)) if v_len(v_sub(p_end, p_start)) > 1e-4 else axis
        total_len = seg_len * phalanges
        segs_data = []
        for k in range(phalanges):
            a = BONES_SPEC[chain[k]][2]
            b = BONES_SPEC[chain[k + 1]][2] if k + 1 < phalanges else v_add(a, v_mul(f_dir, seg_len))
            segs_data.append((chain[k], a, b, rad * 1.45, 1.0))
        segs_data.append((hand_idx, wrist_pos, p_start, rad * 1.8, 0.55))

        _tube_along_axis(
            center=p_start, axis_dir=f_dir, up_hint=(0.0, 0.0, 1.0),
            rx=rad, ry=rad * 0.92, length_a=-0.015, length_b=total_len,
            u_steps=u_finger, v_steps=phalanges * 2,
            profile_fn=lambda t: (1.0 - 0.24 * (t ** 1.4), 1.0 - 0.24 * (t ** 1.4), 0.0, 0.0),
            weight_fn=lambda p, r, u, sd=segs_data: compute_capsule_skin_weights(p, sd),
            builder=builder
        )

def _build_torso_and_arms_jacket(builder, style_name, u_torso, v_torso, u_arm, v_arm):
    """
    Sculpts layered industrial jacket / lab coat / tech vest + articulated sleeves & collar.
    Weighted cleanly across Pelvis, Spine, Spine_01, Spine_02, Chest, Shoulders, Arms, Wrists.
    """
    pelvis = BONE_INDEX["Pelvis"]
    spine = BONE_INDEX["Spine"]
    sp1 = BONE_INDEX["Spine_01"]
    sp2 = BONE_INDEX["Spine_02"]
    chest = BONE_INDEX["Chest"]
    neck = BONE_INDEX["Neck"]

    is_labcoat = (style_name == "Outfit_Jacket_LabCoat")
    is_vest = (style_name == "Outfit_Jacket_TechVest")
    y_bot = 0.80 if is_labcoat else 0.905
    y_top = 1.505

    torso_segs = [
        (pelvis, BONES_SPEC[pelvis][2], BONES_SPEC[spine][2], 0.16, 1.0),
        (spine,  BONES_SPEC[spine][2],  BONES_SPEC[sp1][2],   0.16, 1.0),
        (sp1,    BONES_SPEC[sp1][2],    BONES_SPEC[sp2][2],   0.17, 1.0),
        (sp2,    BONES_SPEC[sp2][2],    BONES_SPEC[chest][2], 0.18, 1.0),
        (chest,  BONES_SPEC[chest][2],  BONES_SPEC[neck][2],  0.18, 1.0),
    ]

    rings = []
    for vi in range(v_torso + 1):
        t = vi / float(v_torso)
        y = y_bot + (y_top - y_bot) * t
        # Anatomical athletic chest/waist taper: hip flare -> tapered waist -> broad deltoid shelf -> collar
        if y < 1.02:
            rx = 0.190 + (0.016 if is_labcoat else 0.004)
            rz = 0.130 + (0.014 if is_labcoat else 0.004)
        elif y < 1.24:
            wt = (y - 1.02) / 0.22
            rx = 0.186 - 0.026 * math.sin(wt * math.pi) + (0.008 if is_vest else 0.0)
            rz = 0.128 - 0.014 * math.sin(wt * math.pi)
        elif y < 1.44:
            ct = (y - 1.24) / 0.20
            rx = 0.184 + 0.044 * math.sin(ct * math.pi * 0.65)
            rz = 0.130 + 0.026 * math.sin(ct * math.pi * 0.75)
        else:
            nt = (y - 1.44) / 0.065
            rx = 0.214 * (1.0 - nt) + 0.088 * nt
            rz = 0.140 * (1.0 - nt) + 0.084 * nt

        ring = []
        for ui in range(u_torso):
            ang = (ui / float(u_torso)) * 2.0 * math.pi
            ca, sa = math.cos(ang), math.sin(ang)
            # Center storm-flap zipper placket, twin breast pocket flaps & chest yoke relief on front (-Z)
            placket = 0.0085 * math.exp(-((ca) / 0.075) ** 2) if sa < -0.5 else 0.0
            pocket = 0.0105 * math.exp(-((abs(ca) - 0.42) / 0.15) ** 2) * math.exp(-((y - 1.31) / 0.042) ** 2) if sa < -0.4 else 0.0
            shoulder_yoke = 0.0045 * math.exp(-((y - 1.425) / 0.025) ** 2)
            rib_fold = 0.0038 * math.sin(y * 36.0 + ca * 4.0)
            r_pt = (
                ca * (rx + shoulder_yoke),
                y,
                sa * (rz + (0.015 if sa < 0 else 0.006) + rib_fold) - placket - pocket
            )
            ring.append(r_pt)
        rings.append((ring, t))

    _loft_rings(
        builder, rings, u_torso, closed_v=True,
        weight_fn=lambda p, r, u: compute_capsule_skin_weights(p, torso_segs)
    )

    # Raised tailored collar ring wrapping snugly over upper chest/neck base
    collar_rings = []
    for vi in range(5):
        t = vi / 4.0
        cy = 1.475 + 0.055 * t
        cr = 0.082 + 0.010 * t
        ring = []
        for ui in range(u_torso):
            ang = (ui / float(u_torso)) * 2.0 * math.pi
            ca, sa = math.cos(ang), math.sin(ang)
            v_open = 0.015 * max(0.0, -sa) * t
            ring.append((ca * cr, cy - v_open, sa * cr + 0.004))
        collar_rings.append((ring, t))
    _loft_rings(
        builder, collar_rings, u_torso, closed_v=True,
        weight_fn=lambda p, r, u: ((neck, chest, 0, 0), (0.65, 0.35, 0.0, 0.0))
    )

    # Left & Right Sleeves extending smoothly over Wrist with tailored cuff ridge
    for sprefix in ("Left", "Right"):
        sh = BONE_INDEX[f"{sprefix}Shoulder"]
        ua = BONE_INDEX[f"{sprefix}UpperArm"]
        el = BONE_INDEX[f"{sprefix}Elbow"]
        fa = BONE_INDEX[f"{sprefix}Forearm"]
        wr = BONE_INDEX[f"{sprefix}Wrist"]
        arm_segs = [
            (chest, BONES_SPEC[chest][2], BONES_SPEC[sh][2], 0.14, 0.85),
            (sh, BONES_SPEC[sh][2], BONES_SPEC[ua][2], 0.11, 1.0),
            (ua, BONES_SPEC[ua][2], BONES_SPEC[el][2], 0.080, 1.0),
            (el, BONES_SPEC[el][2], BONES_SPEC[fa][2], 0.072, 1.0),
            (fa, BONES_SPEC[fa][2], BONES_SPEC[wr][2], 0.068, 1.0),
        ]
        p_sh = BONES_SPEC[ua][2]
        p_wr = BONES_SPEC[wr][2]
        sleeve_dir = v_norm(v_sub(p_wr, p_sh))
        sleeve_len = v_len(v_sub(p_wr, p_sh))
        rad = 0.064 if is_labcoat else (0.054 if is_vest else 0.060)
        _tube_along_axis(
            center=p_sh, axis_dir=sleeve_dir, up_hint=(0.0, 0.0, 1.0),
            rx=rad, ry=rad * 0.96, length_a=-0.065, length_b=sleeve_len + 0.006,
            u_steps=u_arm, v_steps=v_arm,
            profile_fn=lambda t: (
                (1.10 - 0.32 * t + (0.05 if t > 0.88 else 0.0)) * (1.0 + 0.04 * math.sin(t * 28.0)),
                (1.10 - 0.32 * t + (0.05 if t > 0.88 else 0.0)) * (1.0 + 0.04 * math.sin(t * 28.0)),
                0.0, 0.0
            ),
            weight_fn=lambda p, r, u, segs=arm_segs: compute_capsule_skin_weights(p, segs),
            builder=builder
        )

def _build_trousers(builder, style_name, u_leg, v_leg):
    """
    Sculpts cargo/utility trousers from Pelvis waistband down both legs to Ankle cuffs,
    including raised side cargo pockets.
    """
    pelvis = BONE_INDEX["Pelvis"]
    spine = BONE_INDEX["Spine"]
    lhip = BONE_INDEX["LeftHip"]
    rhip = BONE_INDEX["RightHip"]

    # Pelvis hip shell & waistband (Y = 0.840..1.005) overlapping deep under jacket hem
    hip_segs = [
        (pelvis, BONES_SPEC[pelvis][2], BONES_SPEC[spine][2], 0.16, 1.0),
        (lhip, BONES_SPEC[lhip][2], BONES_SPEC[BONE_INDEX["LeftThigh"]][2], 0.13, 0.65),
        (rhip, BONES_SPEC[rhip][2], BONES_SPEC[BONE_INDEX["RightThigh"]][2], 0.13, 0.65),
    ]
    rings = []
    for vi in range(8):
        t = vi / 7.0
        y = 0.840 + 0.165 * t
        rx = 0.184 - 0.012 * t
        rz = 0.124 - 0.008 * t
        rings.append((_superellipse_ring(0.0, y, 0.004, rx, rz, u_leg * 2, front_bulge=0.005, back_bulge=0.016), t))
    _loft_rings(
        builder, rings, u_leg * 2, closed_v=True,
        weight_fn=lambda p, r, u: compute_capsule_skin_weights(p, hip_segs)
    )

    is_cargo = (style_name == "Outfit_Trousers_Cargo")
    for sprefix, sign in (("Left", -1.0), ("Right", 1.0)):
        hip = BONE_INDEX[f"{sprefix}Hip"]
        th = BONE_INDEX[f"{sprefix}Thigh"]
        kn = BONE_INDEX[f"{sprefix}Knee"]
        cf = BONE_INDEX[f"{sprefix}Calf"]
        ak = BONE_INDEX[f"{sprefix}Ankle"]
        leg_segs = [
            (pelvis, BONES_SPEC[pelvis][2], BONES_SPEC[hip][2], 0.14, 0.5),
            (hip, BONES_SPEC[hip][2], BONES_SPEC[th][2], 0.11, 0.9),
            (th,  BONES_SPEC[th][2],  BONES_SPEC[kn][2], 0.09, 1.0),
            (kn,  BONES_SPEC[kn][2],  BONES_SPEC[cf][2], 0.08, 1.0),
            (cf,  BONES_SPEC[cf][2],  BONES_SPEC[ak][2], 0.075, 1.0),
        ]
        rings = []
        for vi in range(v_leg + 1):
            t = vi / float(v_leg)
            y = 0.90 - 0.82 * t  # 0.90 -> 0.08 tucked cleanly inside boots!
            cx = sign * 0.098
            cz = 0.004 + 0.010 * math.sin(t * math.pi)
            if t < 0.45:
                lt = t / 0.45
                r = 0.095 - 0.026 * (lt ** 1.3)
            elif t < 0.72:
                lt = (t - 0.45) / 0.27
                r = 0.069 + 0.006 * math.sin(lt * math.pi)
            else:
                lt = (t - 0.72) / 0.28
                r = 0.069 - 0.014 * lt
            pocket = 0.015 * math.exp(-((t - 0.28) / 0.11) ** 2) if is_cargo else 0.0
            ring = []
            for ui in range(u_leg):
                ang = (ui / float(u_leg)) * 2.0 * math.pi
                ca, sa = math.cos(ang), math.sin(ang)
                outer = max(0.0, ca * sign)
                ring.append((cx + ca * (r + pocket * outer), y, cz + sa * (r * 0.98)))
            rings.append((ring, t))
        _loft_rings(
            builder, rings, u_leg, closed_v=True,
            weight_fn=lambda p, r, u, segs=leg_segs: compute_capsule_skin_weights(p, segs)
        )

def _build_boots(builder, style_name, u_boot, v_boot):
    """
    Sculpts articulated work boots covering Ankle -> Foot -> Toes, with heel, sole,
    toe box, cuff overlapping trousers cleanly, and tread relief. Grounded cleanly at Y=0.0.
    """
    is_tech = (style_name == "Outfit_Boots_Tech")
    for sprefix, sign in (("Left", -1.0), ("Right", 1.0)):
        cf = BONE_INDEX[f"{sprefix}Calf"]
        ak = BONE_INDEX[f"{sprefix}Ankle"]
        ft = BONE_INDEX[f"{sprefix}Foot"]
        to = BONE_INDEX[f"{sprefix}Toes"]
        cx = sign * 0.098
        boot_segs = [
            (cf, BONES_SPEC[cf][2], BONES_SPEC[ak][2], 0.07, 0.4),
            (ak, BONES_SPEC[ak][2], BONES_SPEC[ft][2], 0.065, 1.0),
            (ft, BONES_SPEC[ft][2], BONES_SPEC[to][2], 0.065, 1.0),
            (to, BONES_SPEC[to][2], (cx, 0.015, -0.155), 0.055, 1.0),
        ]

        # Vertical ankle collar cuff wrapping smoothly OUTSIDE lower trouser leg (Y = 0.050..0.195)
        collar_rings = []
        shaft_h = 0.145 if is_tech else 0.125
        for vi in range(5):
            t = vi / 4.0
            y = 0.050 + shaft_h * t
            rx = 0.064 - 0.002 * t + (0.004 if t > 0.78 else 0.0)
            rz = 0.070 - 0.002 * t + (0.004 if t > 0.78 else 0.0)
            collar_rings.append((_superellipse_ring(cx, y, 0.008, rx, rz, u_boot), t))
        _loft_rings(
            builder, collar_rings, u_boot, closed_v=True,
            weight_fn=lambda p, r, u, segs=boot_segs: compute_capsule_skin_weights(p, segs)
        )

        # Foot vamp + toe box + tread sole from heel (Z=+0.068) to toe tip (Z=-0.165)
        foot_rings = []
        for vi in range(v_boot + 1):
            t = vi / float(v_boot)
            z = 0.068 - 0.233 * t  # heel -> toe
            # Cross-section: sole bottom strictly at Y >= 0.000
            if t < 0.25:
                rx = 0.048 + 0.005 * (t / 0.25)
                h = 0.088
            elif t < 0.72:
                rx = 0.054 + 0.004 * math.sin(((t - 0.25) / 0.47) * math.pi)
                h = 0.086 - 0.032 * ((t - 0.25) / 0.47)
            else:
                lt = (t - 0.72) / 0.28
                rx = 0.054 * (1.0 - 0.55 * (lt ** 1.7))
                h = 0.054 * (1.0 - 0.45 * (lt ** 1.5))
            # Sole lug tread bumps along bottom
            tread_groove = 0.004 if (int(t * 14) % 2 == 0) else 0.0
            ring = []
            for ui in range(u_boot):
                ang = (ui / float(u_boot)) * 2.0 * math.pi
                ca, sa = math.cos(ang), math.sin(ang)
                # sa < 0 -> bottom sole, sa > 0 -> upper boot arch
                if sa < 0.0:
                    y = max(0.0, 0.022 + sa * (0.022 - tread_groove))
                else:
                    y = 0.022 + sa * (h - 0.022)
                ring.append((cx + ca * rx, y, z))
            foot_rings.append((ring, t))
        _loft_rings(
            builder, foot_rings, u_boot, closed_v=True,
            weight_fn=lambda p, r, u, segs=boot_segs: compute_capsule_skin_weights(p, segs)
        )

# ==============================================================================
# 6. Procedural Embedded PBR Texture Generator (PNG)
# ==============================================================================

def _png_encode_rgba(width, height, rgba_bytes):
    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    raw = bytearray()
    stride = width * 4
    for y in range(height):
        raw.append(0)  # Filter type None
        raw.extend(rgba_bytes[y * stride:(y + 1) * stride])
    idat = zlib.compress(bytes(raw), level=6)
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", idat) + chunk(b"IEND", b"")

def _hash2(x, y):
    n = math.sin(x * 127.1 + y * 311.7) * 43758.5453
    return n - math.floor(n)

def generate_character_textures(tex_size=128):
    """Generates Albedo, Normal, and MRA PNG byte blobs per character material class."""
    textures = {}

    def make_triplets(name, sample_fn):
        albedo = bytearray(tex_size * tex_size * 4)
        normal = bytearray(tex_size * tex_size * 4)
        mra = bytearray(tex_size * tex_size * 4)
        for y in range(tex_size):
            v = y / float(tex_size)
            for x in range(tex_size):
                u = x / float(tex_size)
                (r, g, b), (nx, ny, nz), (met, rgh, ao) = sample_fn(u, v, x, y)
                idx = (y * tex_size + x) * 4
                albedo[idx:idx+4] = bytes((
                    max(0, min(255, int(r * 255))),
                    max(0, min(255, int(g * 255))),
                    max(0, min(255, int(b * 255))),
                    255
                ))
                nl = math.sqrt(nx*nx + ny*ny + nz*nz) or 1.0
                normal[idx:idx+4] = bytes((
                    max(0, min(255, int((nx / nl * 0.5 + 0.5) * 255))),
                    max(0, min(255, int((ny / nl * 0.5 + 0.5) * 255))),
                    max(0, min(255, int((nz / nl * 0.5 + 0.5) * 255))),
                    255
                ))
                mra[idx:idx+4] = bytes((
                    max(0, min(255, int(met * 255))),
                    max(0, min(255, int(rgh * 255))),
                    max(0, min(255, int(ao * 255))),
                    255
                ))
        textures[f"{name}_albedo"] = _png_encode_rgba(tex_size, tex_size, albedo)
        textures[f"{name}_normal"] = _png_encode_rgba(tex_size, tex_size, normal)
        textures[f"{name}_mra"] = _png_encode_rgba(tex_size, tex_size, mra)

    # 1. Human Skin (white neutral base multiplied by SkinTone tint in shader, pores + fine dermal relief)
    def skin_fn(u, v, x, y):
        pore = (_hash2(x * 0.85, y * 0.85) - 0.5) * 0.042
        micro = math.sin(u * 105.0) * math.cos(v * 105.0) * 0.018
        lip_tint = math.exp(-((v - 0.30) / 0.04) ** 2) * math.exp(-((u - 0.5) / 0.14) ** 2) * 0.06
        base = 0.94 + pore + micro - lip_tint * 0.4
        nx = (math.cos(u * 130.0) * 0.10 + pore * 1.6)
        ny = (math.sin(v * 130.0) * 0.10 + pore * 1.6)
        return (base + lip_tint * 0.4, base * 0.96 - lip_tint * 0.3, base * 0.94 - lip_tint * 0.4), (nx, ny, 1.0), (0.0, 0.52 + pore * 0.7, 0.93)

    # 2. Iris & Sclera Eye Texture (centered at u=0.5, v=0.5)
    def eye_fn(u, v, x, y):
        dx = u - 0.5
        dy = v - 0.5
        r = math.sqrt(dx * dx + dy * dy)
        ang = math.atan2(dy, dx)
        if r < 0.092:
            return (0.03, 0.03, 0.04), (0.0, 0.0, 1.0), (0.0, 0.06, 1.0)
        elif r < 0.255:
            limbal = max(0.0, (r - 0.215) / 0.040)
            collar = 0.12 * math.exp(-((r - 0.125) / 0.025) ** 2)
            fibers = 0.70 + 0.26 * math.sin(ang * 38.0 + r * 44.0) + collar + 0.08 * _hash2(ang * 9, r * 22)
            c = fibers * (1.0 - limbal * 0.72)
            return (c, c, c), (math.cos(ang) * 0.08, math.sin(ang) * 0.08, 1.0), (0.0, 0.08, 1.0)
        else:
            edge_ao = max(0.76, 1.0 - max(0.0, r - 0.29) * 0.85)
            return (0.95 * edge_ao, 0.94 * edge_ao, 0.93 * edge_ao), (0.0, 0.0, 1.0), (0.0, 0.12, edge_ao)

    # 3. Hair Strands (anisotropic vertical hair fibers)
    def hair_fn(u, v, x, y):
        strand = math.sin(u * 230.0 + math.sin(v * 14.0) * 1.6) * 0.5 + 0.5
        fine = _hash2(x * 1.4, y * 0.25)
        shade = 0.74 + 0.20 * strand + 0.08 * fine
        nx = math.cos(u * 230.0) * 0.28
        return (shade, shade, shade), (nx, 0.05, 1.0), (0.02, 0.40 + (1.0 - strand) * 0.20, 0.85 + 0.13 * strand)

    # 4. Ripstop Industrial Fabric (Jacket)
    def jacket_fn(u, v, x, y):
        grid = 1.0 if (x % 5 == 0 or y % 5 == 0) else 0.0
        weave = 0.89 + 0.07 * ((x + y) % 2) - 0.10 * grid
        seam = math.exp(-((abs(u - 0.5) * 38.0) ** 2)) * 0.18
        panel_seam = 0.10 if (abs(v - 0.35) < 0.015 or abs(v - 0.72) < 0.015) else 0.0
        nx = 0.18 * (1 if x % 5 == 0 else (-1 if x % 5 == 1 else 0))
        ny = 0.18 * (1 if y % 5 == 0 else (-1 if y % 5 == 1 else 0))
        col = weave - seam - panel_seam
        return (col, col, col), (nx, ny, 1.0), (0.04, 0.70 - 0.09 * grid, 0.92 - seam - panel_seam)

    # 5. Heavy Cargo Twill (Trousers)
    def trouser_fn(u, v, x, y):
        twill = math.sin((u + v * 0.75) * 170.0) * 0.5 + 0.5
        grain = (_hash2(x, y) - 0.5) * 0.06
        val = 0.82 + 0.12 * twill + grain
        nx = math.cos((u + v * 0.75) * 170.0) * 0.19
        return (val, val, val), (nx, nx * 0.7, 1.0), (0.02, 0.80, 0.89 + 0.09 * twill)

    # 6. Pebbled Tactical Leather (Gloves & Boots)
    def leather_fn(u, v, x, y):
        pebble = math.sin(u * 105.0) * math.sin(v * 105.0) * 0.5 + 0.5
        grain = _hash2(x * 0.75, y * 0.75)
        val = 0.78 + 0.14 * pebble + 0.06 * grain
        nx = math.cos(u * 105.0) * 0.22
        ny = math.cos(v * 105.0) * 0.22
        return (val, val, val), (nx, ny, 1.0), (0.06, 0.50 + 0.16 * (1.0 - pebble), 0.89)

    make_triplets("skin", skin_fn)
    make_triplets("eye", eye_fn)
    make_triplets("hair", hair_fn)
    make_triplets("jacket", jacket_fn)
    make_triplets("trouser", trouser_fn)
    make_triplets("leather", leather_fn)
    return textures

# ==============================================================================
# 7. Embedded Keyframe Animation Generator
# ==============================================================================

def build_animations(bone_nodes_start_idx):
    """
    Produces glTF animation clips with bone rotation quaternions and pelvis translation tracks.
    Clips cover: Idle_Neutral, Idle_Shift, Walk_Fwd, Walk_Back, Walk_Left, Walk_Right,
                 Jog_Fwd, Sprint_Fwd, Jump_Start, Jump_Air, Landing,
                 Crouch_Idle, Crouch_Walk, Reach_Pick, Hold_Carry, Tool_Use, Button_Press, Inspect_Item.
    """
    clips = []

    def track_rot(bone_name, times, eulers):
        return ("rotation", BONE_INDEX[bone_name] + bone_nodes_start_idx, times, [q_euler_xyz(*e) for e in eulers])

    def track_pos(bone_name, times, positions):
        return ("translation", BONE_INDEX[bone_name] + bone_nodes_start_idx, times, positions)

    pelvis_bind = BONES_SPEC[BONE_INDEX["Pelvis"]][2]

    # 1. Idle_Neutral (4s subtle breathing + organic sway)
    t_idle = [0.0, 1.0, 2.0, 3.0, 4.0]
    clips.append(("Idle_Neutral", [
        track_pos("Pelvis", t_idle, [
            pelvis_bind, (0.0, pelvis_bind[1]+0.004, 0.0), pelvis_bind,
            (0.0, pelvis_bind[1]-0.003, 0.0), pelvis_bind
        ]),
        track_rot("Chest", t_idle, [(0.02,0,0), (-0.015,0.01,0), (0.02,0,0), (-0.01,-0.01,0), (0.02,0,0)]),
        track_rot("Head", t_idle, [(0.01,0.02,0), (-0.01,0.04,0.01), (0.0,-0.02,0), (0.01,-0.03,-0.01), (0.01,0.02,0)]),
        track_rot("LeftUpperArm", t_idle, [(0.04,0,0.68), (0.05,0,0.66), (0.04,0,0.68), (0.03,0,0.69), (0.04,0,0.68)]),
        track_rot("RightUpperArm", t_idle, [(0.04,0,-0.68), (0.03,0,-0.69), (0.04,0,-0.68), (0.05,0,-0.66), (0.04,0,-0.68)]),
        track_rot("LeftElbow", t_idle, [(-0.22,0,0)] * 5),
        track_rot("RightElbow", t_idle, [(-0.22,0,0)] * 5),
    ]))

    # 2. Idle_Shift (3s weight shift + look around)
    t_shift = [0.0, 0.8, 1.6, 2.4, 3.0]
    clips.append(("Idle_Shift", [
        track_pos("Pelvis", t_shift, [
            pelvis_bind, (0.03, pelvis_bind[1]-0.01, 0.0), (0.03, pelvis_bind[1]-0.01, 0.0),
            (-0.02, pelvis_bind[1]-0.006, 0.0), pelvis_bind
        ]),
        track_rot("Pelvis", t_shift, [(0,0,0), (0,0.06,-0.03), (0,0.06,-0.03), (0,-0.05,0.02), (0,0,0)]),
        track_rot("Head", t_shift, [(0,0,0), (-0.03,0.16,0.02), (-0.02,0.12,0), (0.02,-0.15,-0.02), (0,0,0)]),
        track_rot("LeftUpperArm", t_shift, [(0.05,0,0.67)]*5),
        track_rot("RightUpperArm", t_shift, [(0.05,0,-0.67)]*5),
    ]))

    # 3-6. Locomotion Cycle Helper (Walk / Jog / Sprint)
    def make_gait(name, cycle_dur, stride_hip, knee_bend, arm_swing, arm_drop, bob, lean, ankle_amp):
        ts = [0.0, cycle_dur*0.25, cycle_dur*0.5, cycle_dur*0.75, cycle_dur]
        # Note: forward swing is -X pitch rotation for thighs/arms (since forward is -Z!)
        return (name, [
            track_pos("Pelvis", ts, [
                (0.0, pelvis_bind[1] + bob, 0.0),
                (0.012, pelvis_bind[1] - bob, 0.0),
                (0.0, pelvis_bind[1] + bob, 0.0),
                (-0.012, pelvis_bind[1] - bob, 0.0),
                (0.0, pelvis_bind[1] + bob, 0.0),
            ]),
            track_rot("Pelvis", ts, [(lean*0.3, 0.06, 0.0), (lean*0.3, 0.0, -0.025), (lean*0.3, -0.06, 0.0), (lean*0.3, 0.0, 0.025), (lean*0.3, 0.06, 0.0)]),
            track_rot("Spine_01", ts, [(lean*0.7, -0.05, 0.0), (lean*0.7, 0.0, 0.02), (lean*0.7, 0.05, 0.0), (lean*0.7, 0.0, -0.02), (lean*0.7, -0.05, 0.0)]),
            track_rot("LeftThigh", ts, [(stride_hip,0,0), (-0.10,0,0), (-stride_hip*0.9,0,0), (0.25*stride_hip,0,0), (stride_hip,0,0)]),
            track_rot("RightThigh", ts, [(-stride_hip*0.9,0,0), (0.25*stride_hip,0,0), (stride_hip,0,0), (-0.10,0,0), (-stride_hip*0.9,0,0)]),
            track_rot("LeftKnee", ts, [(knee_bend*0.15,0,0), (knee_bend*0.95,0,0), (knee_bend*0.20,0,0), (knee_bend*0.40,0,0), (knee_bend*0.15,0,0)]),
            track_rot("RightKnee", ts, [(knee_bend*0.20,0,0), (knee_bend*0.40,0,0), (knee_bend*0.15,0,0), (knee_bend*0.95,0,0), (knee_bend*0.20,0,0)]),
            track_rot("LeftAnkle", ts, [(-ankle_amp,0,0), (ankle_amp*0.6,0,0), (ankle_amp*0.8,0,0), (-ankle_amp*0.4,0,0), (-ankle_amp,0,0)]),
            track_rot("RightAnkle", ts, [(ankle_amp*0.8,0,0), (-ankle_amp*0.4,0,0), (-ankle_amp,0,0), (ankle_amp*0.6,0,0), (ankle_amp*0.8,0,0)]),
            track_rot("LeftUpperArm", ts, [(-arm_swing,0,arm_drop), (0.0,0,arm_drop), (arm_swing,0,arm_drop), (0.0,0,arm_drop), (-arm_swing,0,arm_drop)]),
            track_rot("RightUpperArm", ts, [(arm_swing,0,-arm_drop), (0.0,0,-arm_drop), (-arm_swing,0,-arm_drop), (0.0,0,-arm_drop), (arm_swing,0,-arm_drop)]),
            track_rot("LeftElbow", ts, [(-0.45,0,0), (-0.65,0,0), (-0.30,0,0), (-0.50,0,0), (-0.45,0,0)]),
            track_rot("RightElbow", ts, [(-0.30,0,0), (-0.50,0,0), (-0.45,0,0), (-0.65,0,0), (-0.30,0,0)]),
        ])

    clips.append(make_gait("Walk_Fwd",   1.00, stride_hip=0.46, knee_bend=0.68, arm_swing=0.38, arm_drop=0.66, bob=0.022, lean=0.06, ankle_amp=0.20))
    clips.append(make_gait("Walk_Back",  1.05, stride_hip=-0.38, knee_bend=0.58, arm_swing=-0.28, arm_drop=0.68, bob=0.018, lean=-0.04, ankle_amp=0.16))
    clips.append(make_gait("Jog_Fwd",    0.72, stride_hip=0.72, knee_bend=1.08, arm_swing=0.68, arm_drop=0.60, bob=0.042, lean=0.14, ankle_amp=0.28))
    clips.append(make_gait("Sprint_Fwd", 0.54, stride_hip=0.95, knee_bend=1.35, arm_swing=0.92, arm_drop=0.55, bob=0.056, lean=0.24, ankle_amp=0.34))

    # Strafe Left/Right
    def make_strafe(name, dir_sign):
        ts = [0.0, 0.25, 0.5, 0.75, 1.0]
        return (name, [
            track_pos("Pelvis", ts, [
                pelvis_bind, (0.0, pelvis_bind[1]-0.018, 0.0), pelvis_bind,
                (0.0, pelvis_bind[1]-0.018, 0.0), pelvis_bind
            ]),
            track_rot("Pelvis", ts, [(0.05, 0.14*dir_sign, -0.05*dir_sign)]*5),
            track_rot("LeftThigh", ts, [(0,0,0.28*dir_sign), (0.15,0,0), (0,0,-0.18*dir_sign), (-0.1,0,0.12*dir_sign), (0,0,0.28*dir_sign)]),
            track_rot("RightThigh", ts, [(0,0,-0.18*dir_sign), (-0.1,0,0.12*dir_sign), (0,0,0.28*dir_sign), (0.15,0,0), (0,0,-0.18*dir_sign)]),
            track_rot("LeftKnee", ts, [(0.25,0,0), (0.55,0,0), (0.25,0,0), (0.45,0,0), (0.25,0,0)]),
            track_rot("RightKnee", ts, [(0.25,0,0), (0.45,0,0), (0.25,0,0), (0.55,0,0), (0.25,0,0)]),
            track_rot("LeftUpperArm", ts, [(0.05,0,0.58)]*5),
            track_rot("RightUpperArm", ts, [(0.05,0,-0.58)]*5),
        ])

    clips.append(make_strafe("Walk_Left", 1.0))
    clips.append(make_strafe("Walk_Right", -1.0))

    # Jump_Start, Jump_Air, Landing
    tj = [0.0, 0.18, 0.35]
    clips.append(("Jump_Start", [
        track_pos("Pelvis", tj, [pelvis_bind, (0, pelvis_bind[1]-0.14, 0.02), (0, pelvis_bind[1]+0.03, 0)]),
        track_rot("LeftThigh", tj, [(0,0,0), (0.55,0,0), (-0.25,0,0)]),
        track_rot("RightThigh", tj, [(0,0,0), (0.55,0,0), (-0.15,0,0)]),
        track_rot("LeftKnee", tj, [(0,0,0), (0.85,0,0), (0.20,0,0)]),
        track_rot("RightKnee", tj, [(0,0,0), (0.85,0,0), (0.20,0,0)]),
        track_rot("LeftUpperArm", tj, [(0.04,0,0.68), (-0.25,0,0.55), (0.65,0,0.45)]),
        track_rot("RightUpperArm", tj, [(0.04,0,-0.68), (-0.25,0,-0.55), (0.65,0,-0.45)]),
    ]))
    ta = [0.0, 0.5, 1.0]
    clips.append(("Jump_Air", [
        track_pos("Pelvis", ta, [(0, pelvis_bind[1]+0.04, 0)]*3),
        track_rot("LeftThigh", ta, [(0.45,0,0), (0.52,0,0), (0.45,0,0)]),
        track_rot("RightThigh", ta, [(0.20,0,0), (0.28,0,0), (0.20,0,0)]),
        track_rot("LeftKnee", ta, [(0.92,0,0), (0.85,0,0), (0.92,0,0)]),
        track_rot("RightKnee", ta, [(0.55,0,0), (0.62,0,0), (0.55,0,0)]),
        track_rot("LeftUpperArm", ta, [(0.15,0,0.32), (0.22,0,0.28), (0.15,0,0.32)]),
        track_rot("RightUpperArm", ta, [(0.15,0,-0.32), (0.22,0,-0.28), (0.15,0,-0.32)]),
    ]))
    tl = [0.0, 0.14, 0.38]
    clips.append(("Landing", [
        track_pos("Pelvis", tl, [(0, pelvis_bind[1]-0.05, 0), (0, pelvis_bind[1]-0.18, 0.02), pelvis_bind]),
        track_rot("Spine_01", tl, [(0.12,0,0), (0.24,0,0), (0.02,0,0)]),
        track_rot("LeftThigh", tl, [(0.35,0,0), (0.72,0,0), (0.04,0,0)]),
        track_rot("RightThigh", tl, [(0.35,0,0), (0.72,0,0), (0.04,0,0)]),
        track_rot("LeftKnee", tl, [(0.55,0,0), (1.10,0,0), (0.08,0,0)]),
        track_rot("RightKnee", tl, [(0.55,0,0), (1.10,0,0), (0.08,0,0)]),
    ]))

    # Crouch_Idle & Crouch_Walk
    tc = [0.0, 1.0, 2.0]
    clips.append(("Crouch_Idle", [
        track_pos("Pelvis", tc, [(0, pelvis_bind[1]-0.34, 0.04)]*3),
        track_rot("Spine_01", tc, [(0.22,0,0), (0.24,0,0), (0.22,0,0)]),
        track_rot("LeftThigh", tc, [(0.92,0,0.06)]*3),
        track_rot("RightThigh", tc, [(0.82,0,-0.06)]*3),
        track_rot("LeftKnee", tc, [(1.35,0,0)]*3),
        track_rot("RightKnee", tc, [(1.30,0,0)]*3),
        track_rot("LeftAnkle", tc, [(-0.42,0,0)]*3),
        track_rot("RightAnkle", tc, [(-0.42,0,0)]*3),
        track_rot("LeftUpperArm", tc, [(0.12,0,0.58)]*3),
        track_rot("RightUpperArm", tc, [(0.12,0,-0.58)]*3),
    ]))
    tcw = [0.0, 0.3, 0.6, 0.9, 1.2]
    clips.append(("Crouch_Walk", [
        track_pos("Pelvis", tcw, [(0, pelvis_bind[1]-0.33, 0.04), (0, pelvis_bind[1]-0.36, 0.04)]*2 + [(0, pelvis_bind[1]-0.33, 0.04)]),
        track_rot("Spine_01", tcw, [(0.26,0,0)]*5),
        track_rot("LeftThigh", tcw, [(1.12,0,0.05), (0.85,0,0.05), (0.62,0,0.05), (0.90,0,0.05), (1.12,0,0.05)]),
        track_rot("RightThigh", tcw, [(0.62,0,-0.05), (0.90,0,-0.05), (1.12,0,-0.05), (0.85,0,-0.05), (0.62,0,-0.05)]),
        track_rot("LeftKnee", tcw, [(1.18,0,0), (1.42,0,0), (1.10,0,0), (1.35,0,0), (1.18,0,0)]),
        track_rot("RightKnee", tcw, [(1.10,0,0), (1.35,0,0), (1.18,0,0), (1.42,0,0), (1.10,0,0)]),
    ]))

    # Interaction & Tool Clips
    ti = [0.0, 0.25, 0.55]
    clips.append(("Reach_Pick", [
        track_rot("Spine_02", ti, [(0.0,0,0), (0.16, -0.10, 0), (0.04, 0, 0)]),
        track_rot("RightUpperArm", ti, [(0.04,0,-0.68), (0.72, 0.20, -0.25), (0.35, 0.10, -0.45)]),
        track_rot("RightElbow", ti, [(-0.22,0,0), (-0.55,0,0), (-0.68,0,0)]),
    ]))
    th = [0.0, 1.0, 2.0]
    clips.append(("Hold_Carry", [
        track_rot("RightUpperArm", th, [(0.45, 0.22, -0.42)]*3),
        track_rot("RightElbow", th, [(-0.92, 0, 0)]*3),
        track_rot("LeftUpperArm", th, [(0.35, -0.18, 0.45)]*3),
        track_rot("LeftElbow", th, [(-0.82, 0, 0)]*3),
    ]))
    tu = [0.0, 0.14, 0.30, 0.48]
    clips.append(("Tool_Use", [
        track_rot("Spine_02", tu, [(0.04,-0.06,0), (0.14,-0.12,0), (0.06,0.06,0), (0.04,-0.06,0)]),
        track_rot("RightUpperArm", tu, [(0.42, 0.18, -0.35), (0.85, 0.25, -0.22), (0.12, 0.10, -0.38), (0.42, 0.18, -0.35)]),
        track_rot("RightElbow", tu, [(-0.78, 0, 0), (-1.10, 0, 0), (-0.42, 0, 0), (-0.78, 0, 0)]),
        track_rot("RightWrist", tu, [(0,0,0), (0.28,0,0), (-0.32,0,0), (0,0,0)]),
    ]))
    tb = [0.0, 0.20, 0.45]
    clips.append(("Button_Press", [
        track_rot("RightUpperArm", tb, [(0.15, 0.12, -0.52), (0.82, 0.18, -0.20), (0.15, 0.12, -0.52)]),
        track_rot("RightElbow", tb, [(-0.65,0,0), (-0.18,0,0), (-0.65,0,0)]),
    ]))
    tins = [0.0, 0.7, 1.4]
    clips.append(("Inspect_Item", [
        track_rot("Head", tins, [(0.14,0.08,0), (0.18,-0.08,0), (0.14,0.08,0)]),
        track_rot("LeftUpperArm", tins, [(0.52,-0.25,0.38), (0.58,-0.20,0.34), (0.52,-0.25,0.38)]),
        track_rot("LeftElbow", tins, [(-1.12,0,0), (-1.18,0,0), (-1.12,0,0)]),
        track_rot("RightUpperArm", tins, [(0.45,0.22,-0.40), (0.50,0.28,-0.36), (0.45,0.22,-0.40)]),
        track_rot("RightElbow", tins, [(-0.98,0,0), (-1.05,0,0), (-0.98,0,0)]),
    ]))

    return clips

# ==============================================================================
# 8. glTF 2.0 Binary (.glb) Serializer
# ==============================================================================

def build_glb_for_lod(lod_name, out_path, lod_cfg):
    bin_buf = bytearray()
    buffer_views = []
    accessors = []
    images = []
    textures = []
    materials = []
    meshes = []
    nodes = []

    def append_bytes(raw_bytes, target=None):
        while len(bin_buf) % 4 != 0:
            bin_buf.append(0)
        offset = len(bin_buf)
        bin_buf.extend(raw_bytes)
        bv_idx = len(buffer_views)
        bv = {"buffer": 0, "byteOffset": offset, "byteLength": len(raw_bytes)}
        if target is not None:
            bv["target"] = target
        buffer_views.append(bv)
        return bv_idx

    def add_accessor(bv_idx, comp_type, count, acc_type, min_v=None, max_v=None, normalized=False):
        idx = len(accessors)
        acc = {
            "bufferView": bv_idx,
            "componentType": comp_type,
            "count": count,
            "type": acc_type,
        }
        if normalized:
            acc["normalized"] = True
        if min_v is not None:
            acc["min"] = min_v
        if max_v is not None:
            acc["max"] = max_v
        accessors.append(acc)
        return idx

    # 1. Embed PBR textures (albedo, normal, mra)
    tex_blobs = generate_character_textures(tex_size=lod_cfg["tex_size"])
    mat_names = ["skin", "eye", "hair", "jacket", "trouser", "leather"]
    mat_map = {}

    for m_idx, mname in enumerate(mat_names):
        def add_tex(png_bytes):
            bv = append_bytes(png_bytes)
            img_idx = len(images)
            images.append({"bufferView": bv, "mimeType": "image/png"})
            t_idx = len(textures)
            textures.append({"source": img_idx})
            return t_idx

        alb_t = add_tex(tex_blobs[f"{mname}_albedo"])
        nrm_t = add_tex(tex_blobs[f"{mname}_normal"])
        mra_t = add_tex(tex_blobs[f"{mname}_mra"])

        rough_def = {"skin": 0.56, "eye": 0.12, "hair": 0.44, "jacket": 0.72, "trouser": 0.82, "leather": 0.54}[mname]
        met_def = 0.02 if mname in ("jacket", "leather") else 0.0
        materials.append({
            "name": mname,
            "pbrMetallicRoughness": {
                "baseColorTexture": {"index": alb_t},
                "metallicFactor": met_def,
                "roughnessFactor": rough_def,
                "metallicRoughnessTexture": {"index": mra_t},
            },
            "normalTexture": {"index": nrm_t},
            "extras": {"materialRole": mname}
        })
        mat_map[mname] = m_idx

    # 2. Build all anatomical & garment builders
    builders = []

    b_head = PrimitiveBuilder("Body_HeadFaceSkin", mat_map["skin"])
    _build_head_face(b_head, lod_cfg["u_head"], lod_cfg["v_head"])
    b_head.recompute_normals_and_tangents(smooth=True)
    builders.append(b_head)

    b_eyes = PrimitiveBuilder("Face_Eyes", mat_map["eye"])
    _build_eyes(b_eyes, lod_cfg["eye_segs"])
    b_eyes.recompute_normals_and_tangents(smooth=True)
    builders.append(b_eyes)

    b_brows = PrimitiveBuilder("Face_Eyebrows", mat_map["hair"])
    _build_eyebrows(b_brows, max(5, lod_cfg["u_finger"]))
    b_brows.recompute_normals_and_tangents(smooth=True)
    builders.append(b_brows)

    for hname in ("Hair_CrewCut", "Hair_SidePart", "Hair_TiedBack", "Hair_TechCap"):
        bh = PrimitiveBuilder(hname, mat_map["hair"])
        _build_hairstyle(bh, hname, lod_cfg["u_hair"], lod_cfg["v_hair"])
        bh.recompute_normals_and_tangents(smooth=True)
        builders.append(bh)

    for jname in ("Outfit_Jacket_Industrial", "Outfit_Jacket_LabCoat", "Outfit_Jacket_TechVest"):
        bj = PrimitiveBuilder(jname, mat_map["jacket"])
        _build_torso_and_arms_jacket(bj, jname, lod_cfg["u_torso"], lod_cfg["v_torso"], lod_cfg["u_arm"], lod_cfg["v_arm"])
        bj.recompute_normals_and_tangents(smooth=True)
        builders.append(bj)

    for tname in ("Outfit_Trousers_Cargo", "Outfit_Trousers_Utility"):
        bt = PrimitiveBuilder(tname, mat_map["trouser"])
        _build_trousers(bt, tname, lod_cfg["u_leg"], lod_cfg["v_leg"])
        bt.recompute_normals_and_tangents(smooth=True)
        builders.append(bt)

    # Gloves (full 5-finger articulated hands) + Bare Forearm/Hands skin variant
    for gname, gmat, gloved in (("Outfit_Gloves_Tactical", "leather", True), ("Body_HandsSkin", "skin", False)):
        bg = PrimitiveBuilder(gname, mat_map[gmat])
        _build_hand_and_fingers(bg, "Left", -1.0, lod_cfg["u_palm"], lod_cfg["v_palm"], lod_cfg["u_finger"], is_gloved=gloved)
        _build_hand_and_fingers(bg, "Right", 1.0, lod_cfg["u_palm"], lod_cfg["v_palm"], lod_cfg["u_finger"], is_gloved=gloved)
        bg.recompute_normals_and_tangents(smooth=True)
        builders.append(bg)

    for bname in ("Outfit_Boots_Work", "Outfit_Boots_Tech"):
        bb = PrimitiveBuilder(bname, mat_map["leather"])
        _build_boots(bb, bname, lod_cfg["u_boot"], lod_cfg["v_boot"])
        bb.recompute_normals_and_tangents(smooth=True)
        builders.append(bb)

    # Utility Waist Belt & Buckle bridging Jacket hem and Trousers waistband cleanly
    pelvis = BONE_INDEX["Pelvis"]
    spine = BONE_INDEX["Spine"]
    belt_segs = [
        (pelvis, BONES_SPEC[pelvis][2], BONES_SPEC[spine][2], 0.16, 1.0),
    ]
    b_belt = PrimitiveBuilder("Outfit_Belt", mat_map["leather"])
    belt_rings = []
    for vi in range(4):
        t = vi / 3.0
        by = 0.908 + 0.048 * t
        rx = 0.194
        rz = 0.136
        ring = []
        for ui in range(lod_cfg["u_torso"]):
            ang = (ui / float(lod_cfg["u_torso"])) * 2.0 * math.pi
            ca, sa = math.cos(ang), math.sin(ang)
            buckle = 0.010 * math.exp(-(ca / 0.14) ** 2) if sa < -0.6 else 0.0
            ring.append((ca * rx, by, 0.004 + sa * rz - buckle))
        belt_rings.append((ring, t))
    _loft_rings(b_belt, belt_rings, lod_cfg["u_torso"], closed_v=True,
                weight_fn=lambda p, r, u, sd=belt_segs: compute_capsule_skin_weights(p, sd))
    b_belt.recompute_normals_and_tangents(smooth=True)
    builders.append(b_belt)

    # 3. Export Skeleton Nodes + InverseBindMatrices
    # Reserve node 0 as CharacterRoot, nodes 1..65 as Bones, nodes 66.. as Mesh nodes
    bone_nodes_start = 1
    nodes.append({
        "name": "CharacterRoot",
        "children": [bone_nodes_start]  # Root bone + mesh siblings added below
    })

    children_of_bone = {i: [] for i in range(len(BONES_SPEC))}
    for i, (bname, parent_name, _) in enumerate(BONES_SPEC):
        if parent_name is not None:
            children_of_bone[BONE_INDEX[parent_name]].append(i + bone_nodes_start)

    ibm_bytes = bytearray()
    for i, (bname, parent_name, wpos) in enumerate(BONES_SPEC):
        if parent_name is None:
            rel = wpos
        else:
            ppos = BONES_SPEC[BONE_INDEX[parent_name]][2]
            rel = v_sub(wpos, ppos)
        node_dict = {
            "name": bname,
            "translation": [round(rel[0], 6), round(rel[1], 6), round(rel[2], 6)],
        }
        if children_of_bone[i]:
            node_dict["children"] = children_of_bone[i]
        nodes.append(node_dict)

        # InverseBindMatrix: translate(-wpos) in column-major
        ibm = mat4_translate((-wpos[0], -wpos[1], -wpos[2]))
        ibm_bytes.extend(struct.pack("<16f", *ibm))

    ibm_bv = append_bytes(bytes(ibm_bytes))
    ibm_acc = add_accessor(ibm_bv, 5126, len(BONES_SPEC), "MAT4")

    skin_idx = 0
    skins = [{
        "name": "HumanoidRig65",
        "inverseBindMatrices": ibm_acc,
        "skeleton": bone_nodes_start,
        "joints": [bone_nodes_start + i for i in range(len(BONES_SPEC))]
    }]

    # 4. Serialize Meshes & Mesh Nodes
    total_verts = 0
    total_tris = 0
    for bld in builders:
        v_cnt = len(bld.positions)
        t_cnt = len(bld.indices) // 3
        total_verts += v_cnt
        total_tris += t_cnt
        assert v_cnt < 65535, f"Mesh {bld.name} exceeds 16-bit index limit: {v_cnt}"

        pos_min = [min(p[k] for p in bld.positions) for k in range(3)]
        pos_max = [max(p[k] for p in bld.positions) for k in range(3)]

        pos_raw = b"".join(struct.pack("<3f", *p) for p in bld.positions)
        nrm_raw = b"".join(struct.pack("<3f", *n) for n in bld.normals)
        tan_raw = b"".join(struct.pack("<4f", *t) for t in bld.tangents)
        uv_raw  = b"".join(struct.pack("<2f", *u) for u in bld.uvs)
        jnt_raw = b"".join(struct.pack("<4B", *(min(255, max(0, int(x))) for x in j)) for j in bld.joints)
        wgt_raw = b"".join(struct.pack("<4f", *w) for w in bld.weights)
        idx_raw = b"".join(struct.pack("<H", i) for i in bld.indices)

        pos_acc = add_accessor(append_bytes(pos_raw, 34962), 5126, v_cnt, "VEC3", pos_min, pos_max)
        nrm_acc = add_accessor(append_bytes(nrm_raw, 34962), 5126, v_cnt, "VEC3")
        tan_acc = add_accessor(append_bytes(tan_raw, 34962), 5126, v_cnt, "VEC4")
        uv_acc  = add_accessor(append_bytes(uv_raw,  34962), 5126, v_cnt, "VEC2")
        jnt_acc = add_accessor(append_bytes(jnt_raw, 34962), 5121, v_cnt, "VEC4")
        wgt_acc = add_accessor(append_bytes(wgt_raw, 34962), 5126, v_cnt, "VEC4")
        idx_acc = add_accessor(append_bytes(idx_raw, 34963), 5123, len(bld.indices), "SCALAR")

        prim = {
            "attributes": {
                "POSITION": pos_acc,
                "NORMAL": nrm_acc,
                "TANGENT": tan_acc,
                "TEXCOORD_0": uv_acc,
                "JOINTS_0": jnt_acc,
                "WEIGHTS_0": wgt_acc,
            },
            "indices": idx_acc,
            "material": bld.material_idx,
        }

        target_names = list(bld.morph_targets.keys())
        if target_names:
            targets_arr = []
            for mname in target_names:
                deltas = bld.morph_targets[mname]
                dmin = [min(d[k] for d in deltas) for k in range(3)]
                dmax = [max(d[k] for d in deltas) for k in range(3)]
                d_raw = b"".join(struct.pack("<3f", *d) for d in deltas)
                d_acc = add_accessor(append_bytes(d_raw, 34962), 5126, v_cnt, "VEC3", dmin, dmax)
                targets_arr.append({"POSITION": d_acc})
            prim["targets"] = targets_arr

        mesh_idx = len(meshes)
        mesh_dict = {"name": bld.name, "primitives": [prim]}
        if target_names:
            mesh_dict["extras"] = {"targetNames": target_names}
            mesh_dict["weights"] = [0.0] * len(target_names)
        meshes.append(mesh_dict)

        mesh_node_idx = len(nodes)
        nodes.append({"name": bld.name, "mesh": mesh_idx, "skin": skin_idx})
        nodes[0]["children"].append(mesh_node_idx)

    # 5. Serialize Keyframe Animations
    clips = build_animations(bone_nodes_start)
    animations = []
    for clip_name, tracks in clips:
        samplers = []
        channels = []
        for path, target_node, times, values in tracks:
            t_raw = b"".join(struct.pack("<f", t) for t in times)
            t_acc = add_accessor(
                append_bytes(t_raw), 5126, len(times), "SCALAR",
                [min(times)], [max(times)]
            )
            if path == "rotation":
                v_raw = b"".join(struct.pack("<4f", *q) for q in values)
                v_acc = add_accessor(append_bytes(v_raw), 5126, len(values), "VEC4")
            else:
                v_raw = b"".join(struct.pack("<3f", *v) for v in values)
                v_acc = add_accessor(append_bytes(v_raw), 5126, len(values), "VEC3")
            s_idx = len(samplers)
            samplers.append({"input": t_acc, "interpolation": "LINEAR", "output": v_acc})
            channels.append({"sampler": s_idx, "target": {"node": target_node, "path": path}})
        animations.append({"name": clip_name, "samplers": samplers, "channels": channels})

    while len(bin_buf) % 4 != 0:
        bin_buf.append(0)

    gltf = {
        "asset": {
            "version": "2.0",
            "generator": "KineticLab3D-CharacterPipeline-v1.0"
        },
        "scene": 0,
        "scenes": [{"name": f"Operator_{lod_name}", "nodes": [0]}],
        "nodes": nodes,
        "meshes": meshes,
        "skins": skins,
        "materials": materials,
        "textures": textures,
        "images": images,
        "animations": animations,
        "accessors": accessors,
        "bufferViews": buffer_views,
        "buffers": [{"byteLength": len(bin_buf)}],
    }

    json_bytes = json.dumps(gltf, separators=(",", ":")).encode("utf-8")
    while len(json_bytes) % 4 != 0:
        json_bytes += b" "

    total_len = 12 + 8 + len(json_bytes) + 8 + len(bin_buf)
    header = struct.pack("<3I", 0x46546C67, 2, total_len)
    json_chunk = struct.pack("<2I", len(json_bytes), 0x4E4F534A) + json_bytes
    bin_chunk = struct.pack("<2I", len(bin_buf), 0x004E4942) + bytes(bin_buf)

    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    with open(out_path, "wb") as f:
        f.write(header)
        f.write(json_chunk)
        f.write(bin_chunk)

    print(f"[{lod_name}] Wrote {out_path}: {total_verts} verts, {total_tris} tris, {len(BONES_SPEC)} bones, {len(meshes)} submeshes, {len(animations)} anims, {total_len/1024:.1f} KB")

def main():
    base_dir = os.path.join("app", "src", "main", "assets", "characters")
    lods = {
        "HIGH": (
            os.path.join(base_dir, "operator_high.glb"),
            dict(tex_size=128, u_head=28, v_head=24, eye_segs=10, u_hair=24, v_hair=14,
                 u_torso=26, v_torso=18, u_arm=14, v_arm=14, u_leg=16, v_leg=16,
                 u_palm=12, v_palm=6, u_finger=8, u_boot=14, v_boot=12)
        ),
        "MED": (
            os.path.join(base_dir, "operator_med.glb"),
            dict(tex_size=96, u_head=20, v_head=16, eye_segs=8, u_hair=18, v_hair=10,
                 u_torso=18, v_torso=12, u_arm=10, v_arm=10, u_leg=12, v_leg=12,
                 u_palm=8, v_palm=4, u_finger=6, u_boot=10, v_boot=8)
        ),
        "LOW": (
            os.path.join(base_dir, "operator_low.glb"),
            dict(tex_size=64, u_head=14, v_head=10, eye_segs=6, u_hair=12, v_hair=7,
                 u_torso=12, v_torso=8, u_arm=8, v_arm=7, u_leg=8, v_leg=8,
                 u_palm=6, v_palm=3, u_finger=5, u_boot=8, v_boot=6)
        ),
    }
    for name, (path, cfg) in lods.items():
        build_glb_for_lod(name, path, cfg)

if __name__ == "__main__":
    main()
