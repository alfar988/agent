#!/usr/bin/env python3
"""
Phase 7 Procedural Multi-Part Binary glTF 2.0 (.glb) Asset Pipeline
for KineticLab 3D.

Generates ~99 validated multi-material .glb models across:
  - app/src/main/assets/models/firearms/ (6 virtual game firearm categories)
  - app/src/main/assets/models/ammo/     (6 matching caliber ammo crates/boxes)
  - app/src/main/assets/models/tools/    (all 34 industrial/lab/sandbox tools)
  - app/src/main/assets/models/{containers,mechanical,construction,laboratory,
      experimental,furniture,electrical,environment}/ (all 53 prop & architectural meshes)
  - app/src/main/assets/models/manifest.json

Every asset embeds valid glTF 2.0 accessors (POSITION, NORMAL, TANGENT, TEXCOORD_0,
SCALAR indices), multi-material PBR factors, extras.sockets where applicable,
and passes structural validation before disk write.
"""

import json
import math
import os
import struct

PI = math.pi
TAU = 2.0 * math.pi


def v_add(a, b):
    return (a[0] + b[0], a[1] + b[1], a[2] + b[2])


def v_sub(a, b):
    return (a[0] - b[0], a[1] - b[1], a[2] - b[2])


def v_mul(a, s):
    return (a[0] * s, a[1] * s, a[2] * s)


def v_cross(a, b):
    return (
        a[1] * b[2] - a[2] * b[1],
        a[2] * b[0] - a[0] * b[2],
        a[0] * b[1] - a[1] * b[0],
    )


def v_dot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def v_norm(a):
    l = math.sqrt(v_dot(a, a))
    return (a[0] / l, a[1] / l, a[2] / l) if l > 1e-7 else (0.0, 1.0, 0.0)


def q_mul(q1, q2):
    x1, y1, z1, w1 = q1
    x2, y2, z2, w2 = q2
    return (
        w1 * x2 + x1 * w2 + y1 * z2 - z1 * y2,
        w1 * y2 - x1 * z2 + y1 * w2 + z1 * x2,
        w1 * z2 + x1 * y2 - y1 * x2 + z1 * w2,
        w1 * w2 - x1 * x2 - y1 * y2 - z1 * z2,
    )


def q_axis(axis, rad):
    ax = v_norm(axis)
    h = rad * 0.5
    s = math.sin(h)
    return (ax[0] * s, ax[1] * s, ax[2] * s, math.cos(h))


def q_rot(q, v):
    qx, qy, qz, qw = q
    u = (qx, qy, qz)
    t = v_mul(v_cross(u, v), 2.0)
    return v_add(v_add(v, v_mul(t, qw)), v_cross(u, t))


class PartMesh:
    """Single-material triangle mesh primitive ready for glTF primitive export."""

    def __init__(self, name, material_idx=0):
        self.name = name
        self.material_idx = material_idx
        self.positions = []
        self.normals = []
        self.tangents = []
        self.uvs = []
        self.indices = []

    def add_vertex(self, p, n, t, u, v):
        idx = len(self.positions)
        self.positions.append(p)
        nn = v_norm(n)
        self.normals.append(nn)
        tt = v_norm(t)
        self.tangents.append((tt[0], tt[1], tt[2], 1.0))
        self.uvs.append((u, v))
        return idx

    def add_quad(self, p0, p1, p2, p3, n, u_sc=1.0, v_sc=1.0):
        t = v_norm(v_sub(p1, p0))
        i0 = self.add_vertex(p0, n, t, 0.0, 0.0)
        i1 = self.add_vertex(p1, n, t, u_sc, 0.0)
        i2 = self.add_vertex(p2, n, t, u_sc, v_sc)
        i3 = self.add_vertex(p3, n, t, 0.0, v_sc)
        self.indices.extend((i0, i1, i2, i0, i2, i3))

    def add_tri(self, p0, p1, p2, n=None, uv0=(0.0, 0.0), uv1=(1.0, 0.0), uv2=(0.5, 1.0)):
        e1 = v_sub(p1, p0)
        e2 = v_sub(p2, p0)
        nn = v_norm(n if n is not None else v_cross(e1, e2))
        t = v_norm(e1)
        i0 = self.add_vertex(p0, nn, t, uv0[0], uv0[1])
        i1 = self.add_vertex(p1, nn, t, uv1[0], uv1[1])
        i2 = self.add_vertex(p2, nn, t, uv2[0], uv2[1])
        self.indices.extend((i0, i1, i2))

    def add_box(self, cx, cy, cz, hx, hy, hz, rot=None):
        x0, x1 = -hx, hx
        y0, y1 = -hy, hy
        z0, z1 = -hz, hz
        faces = [
            ((x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1), (0, 0, 1), hx * 2, hy * 2),
            ((x1, y0, z0), (x0, y0, z0), (x0, y1, z0), (x1, y1, z0), (0, 0, -1), hx * 2, hy * 2),
            ((x1, y0, z1), (x1, y0, z0), (x1, y1, z0), (x1, y1, z1), (1, 0, 0), hz * 2, hy * 2),
            ((x0, y0, z0), (x0, y0, z1), (x0, y1, z1), (x0, y1, z0), (-1, 0, 0), hz * 2, hy * 2),
            ((x0, y1, z1), (x1, y1, z1), (x1, y1, z0), (x0, y1, z0), (0, 1, 0), hx * 2, hz * 2),
            ((x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1), (0, -1, 0), hx * 2, hz * 2),
        ]
        c = (cx, cy, cz)
        for p0, p1, p2, p3, n, us, vs in faces:
            if rot is not None:
                p0, p1, p2, p3 = q_rot(rot, p0), q_rot(rot, p1), q_rot(rot, p2), q_rot(rot, p3)
                n = q_rot(rot, n)
            self.add_quad(v_add(c, p0), v_add(c, p1), v_add(c, p2), v_add(c, p3), n, us, vs)

    def add_chamfered_box(self, cx, cy, cz, hx, hy, hz, bevel=0.01, rot=None):
        b = min(bevel, min(hx, hy, hz) * 0.35)
        if b < 1e-4:
            self.add_box(cx, cy, cz, hx, hy, hz, rot)
            return
        ix, iy, iz = hx - b, hy - b, hz - b
        inv2 = 0.70710678
        quads = [
            ((-ix, -iy, hz), (ix, -iy, hz), (ix, iy, hz), (-ix, iy, hz), (0, 0, 1)),
            ((ix, -iy, -hz), (-ix, -iy, -hz), (-ix, iy, -hz), (ix, iy, -hz), (0, 0, -1)),
            ((hx, -iy, iz), (hx, -iy, -iz), (hx, iy, -iz), (hx, iy, iz), (1, 0, 0)),
            ((-hx, -iy, -iz), (-hx, -iy, iz), (-hx, iy, iz), (-hx, iy, -iz), (-1, 0, 0)),
            ((-ix, hy, iz), (ix, hy, iz), (ix, hy, -iz), (-ix, hy, -iz), (0, 1, 0)),
            ((-ix, -hy, -iz), (ix, -hy, -iz), (ix, -hy, iz), (-ix, -hy, iz), (0, -1, 0)),
            # Top 4 bevels
            ((-ix, iy, hz), (ix, iy, hz), (ix, hy, iz), (-ix, hy, iz), (0, inv2, inv2)),
            ((ix, iy, -hz), (-ix, iy, -hz), (-ix, hy, -iz), (ix, hy, -iz), (0, inv2, -inv2)),
            ((hx, iy, iz), (hx, iy, -iz), (ix, hy, -iz), (ix, hy, iz), (inv2, inv2, 0)),
            ((-hx, iy, -iz), (-hx, iy, iz), (-ix, hy, iz), (-ix, hy, -iz), (-inv2, inv2, 0)),
            # Bottom 4 bevels
            ((-ix, -hy, iz), (ix, -hy, iz), (ix, -iy, hz), (-ix, -iy, hz), (0, -inv2, inv2)),
            ((ix, -hy, -iz), (-ix, -hy, -iz), (-ix, -iy, -hz), (ix, -iy, -hz), (0, -inv2, -inv2)),
            ((ix, -hy, iz), (ix, -hy, -iz), (hx, -iy, -iz), (hx, -iy, iz), (inv2, -inv2, 0)),
            ((-ix, -hy, -iz), (-ix, -hy, iz), (-hx, -iy, iz), (-hx, -iy, -iz), (-inv2, -inv2, 0)),
            # Vertical 4 bevels
            ((ix, -iy, hz), (hx, -iy, iz), (hx, iy, iz), (ix, iy, hz), (inv2, 0, inv2)),
            ((-hx, -iy, iz), (-ix, -iy, hz), (-ix, iy, hz), (-hx, iy, iz), (-inv2, 0, inv2)),
            ((hx, -iy, -iz), (ix, -iy, -hz), (ix, iy, -hz), (hx, iy, -iz), (inv2, 0, -inv2)),
            ((-ix, -iy, -hz), (-hx, -iy, -iz), (-hx, iy, -iz), (-ix, iy, -hz), (-inv2, 0, -inv2)),
        ]
        c = (cx, cy, cz)
        for p0, p1, p2, p3, n in quads:
            if rot is not None:
                p0, p1, p2, p3 = q_rot(rot, p0), q_rot(rot, p1), q_rot(rot, p2), q_rot(rot, p3)
                n = q_rot(rot, n)
            self.add_quad(v_add(c, p0), v_add(c, p1), v_add(c, p2), v_add(c, p3), n)

    def add_cylinder(self, cx, cy, cz, r0, r1, length, segs=12, axis="y", rot=None):
        half = length * 0.5
        c = (cx, cy, cz)

        def map_pt(ca, sa, r, ax_val):
            if axis == "y":
                p = (ca * r, ax_val, sa * r)
                n = (ca, 0.0, sa)
            elif axis == "z":
                p = (ca * r, sa * r, ax_val)
                n = (ca, sa, 0.0)
            else:
                p = (ax_val, ca * r, sa * r)
                n = (0.0, ca, sa)
            return p, n

        cap0 = (0.0, -half, 0.0) if axis == "y" else ((0.0, 0.0, -half) if axis == "z" else (-half, 0.0, 0.0))
        cap1 = (0.0, half, 0.0) if axis == "y" else ((0.0, 0.0, half) if axis == "z" else (half, 0.0, 0.0))
        n_c0 = (0.0, -1.0, 0.0) if axis == "y" else ((0.0, 0.0, -1.0) if axis == "z" else (-1.0, 0.0, 0.0))
        n_c1 = (0.0, 1.0, 0.0) if axis == "y" else ((0.0, 0.0, 1.0) if axis == "z" else (1.0, 0.0, 0.0))

        for i in range(segs):
            a0 = (i / segs) * TAU
            a1 = ((i + 1) / segs) * TAU
            ca0, sa0 = math.cos(a0), math.sin(a0)
            ca1, sa1 = math.cos(a1), math.sin(a1)
            p00, _ = map_pt(ca0, sa0, r0, -half)
            p10, _ = map_pt(ca1, sa1, r0, -half)
            p11, _ = map_pt(ca1, sa1, r1, half)
            p01, _ = map_pt(ca0, sa0, r1, half)
            _, nm = map_pt(math.cos((a0 + a1) * 0.5), math.sin((a0 + a1) * 0.5), 1.0, 0.0)

            pts = [p00, p10, p11, p01]
            c0p, c1p = cap0, cap1
            nc0, nc1 = n_c0, n_c1
            if rot is not None:
                pts = [q_rot(rot, p) for p in pts]
                nm = q_rot(rot, nm)
                c0p, c1p = q_rot(rot, cap0), q_rot(rot, cap1)
                nc0, nc1 = q_rot(rot, n_c0), q_rot(rot, n_c1)

            wp = [v_add(c, p) for p in pts]
            self.add_quad(wp[0], wp[1], wp[2], wp[3], nm, 1.0 / segs, 1.0)
            self.add_tri(v_add(c, c1p), wp[3], wp[2], nc1)
            self.add_tri(v_add(c, c0p), wp[1], wp[0], nc0)

    def add_torus(self, cx, cy, cz, major_r, minor_r, maj_segs=16, min_segs=8, vertical_x=False):
        c = (cx, cy, cz)
        for i in range(maj_segs):
            u0 = (i / maj_segs) * TAU
            u1 = ((i + 1) / maj_segs) * TAU
            for j in range(min_segs):
                v0 = (j / min_segs) * TAU
                v1 = ((j + 1) / min_segs) * TAU

                def pt(u, v):
                    r = major_r + minor_r * math.cos(v)
                    p = (r * math.cos(u), minor_r * math.sin(v), r * math.sin(u))
                    return (p[1], p[0], p[2]) if vertical_x else p

                p00, p10, p11, p01 = pt(u0, v0), pt(u1, v0), pt(u1, v1), pt(u0, v1)
                um = (u0 + u1) * 0.5
                cm = (0.0, major_r * math.cos(um), major_r * math.sin(um)) if vertical_x else (major_r * math.cos(um), 0.0, major_r * math.sin(um))
                avg = v_mul(v_add(v_add(p00, p10), v_add(p11, p01)), 0.25)
                n = v_norm(v_sub(avg, cm))
                self.add_quad(v_add(c, p00), v_add(c, p10), v_add(c, p11), v_add(c, p01), n)

    def add_sphere(self, cx, cy, cz, radius, rings=12, sectors=18):
        c = (cx, cy, cz)
        for r in range(rings):
            phi0 = (r / rings) * PI
            phi1 = ((r + 1) / rings) * PI
            for s in range(sectors):
                th0 = (s / sectors) * TAU
                th1 = ((s + 1) / sectors) * TAU

                def sp(phi, th):
                    return (
                        radius * math.sin(phi) * math.cos(th),
                        radius * math.cos(phi),
                        radius * math.sin(phi) * math.sin(th),
                    )

                p00, p10, p11, p01 = sp(phi0, th0), sp(phi1, th0), sp(phi1, th1), sp(phi0, th1)
                n = v_norm(v_add(v_add(p00, p10), v_add(p11, p01)))
                if r == 0:
                    self.add_tri(v_add(c, p00), v_add(c, p10), v_add(c, p11), n)
                elif r == rings - 1:
                    self.add_tri(v_add(c, p00), v_add(c, p10), v_add(c, p01), n)
                else:
                    self.add_quad(v_add(c, p00), v_add(c, p11), v_add(c, p01), v_add(c, p10), n)

    def add_coil(self, cx, cy, cz, coil_r, wire_r, height, turns=5.0, steps=36):
        c = (cx, cy, cz)
        sides = 6
        y_start = -height * 0.5
        for i in range(steps):
            t0 = i / steps
            t1 = (i + 1) / steps
            a0 = t0 * turns * TAU
            a1 = t1 * turns * TAU
            c0 = (coil_r * math.cos(a0), y_start + t0 * height, coil_r * math.sin(a0))
            c1 = (coil_r * math.cos(a1), y_start + t1 * height, coil_r * math.sin(a1))
            tan = v_norm(v_sub(c1, c0))
            bi = v_norm(v_cross(tan, (0.0, 1.0, 0.0)))
            nm = v_norm(v_cross(bi, tan))
            for s in range(sides):
                sa0 = (s / sides) * TAU
                sa1 = ((s + 1) / sides) * TAU
                off0 = v_mul(v_add(v_mul(bi, math.cos(sa0)), v_mul(nm, math.sin(sa0))), wire_r)
                off1 = v_mul(v_add(v_mul(bi, math.cos(sa1)), v_mul(nm, math.sin(sa1))), wire_r)
                n = v_norm(v_add(off0, off1))
                self.add_quad(
                    v_add(c, v_add(c0, off0)),
                    v_add(c, v_add(c1, off0)),
                    v_add(c, v_add(c1, off1)),
                    v_add(c, v_add(c0, off1)),
                    n,
                )


class GlbAssetBuilder:
    def __init__(self, name):
        self.name = name
        self.materials = []
        self.parts = []
        self.sockets = {}

    def add_mat(self, name, color=(0.75, 0.75, 0.78), metallic=0.1, roughness=0.55, alpha=1.0, emissive=(0.0, 0.0, 0.0)):
        idx = len(self.materials)
        self.materials.append({
            "name": name,
            "pbrMetallicRoughness": {
                "baseColorFactor": [color[0], color[1], color[2], alpha],
                "metallicFactor": metallic,
                "roughnessFactor": roughness,
            },
            "emissiveFactor": [emissive[0], emissive[1], emissive[2]],
        })
        return idx

    def part(self, name, mat_idx=0):
        p = PartMesh(name, mat_idx)
        self.parts.append(p)
        return p

    def socket(self, key, pos):
        self.sockets[key] = [round(pos[0], 5), round(pos[1], 5), round(pos[2], 5)]

    def export(self, out_path):
        if not self.materials:
            self.add_mat("Default")
        bin_buf = bytearray()
        buffer_views = []
        accessors = []

        def append_bytes(data, target=None):
            while len(bin_buf) % 4 != 0:
                bin_buf.append(0)
            off = len(bin_buf)
            bin_buf.extend(data)
            bv = {"buffer": 0, "byteOffset": off, "byteLength": len(data)}
            if target is not None:
                bv["target"] = target
            idx = len(buffer_views)
            buffer_views.append(bv)
            return idx

        def add_acc(bv_idx, comp_type, count, typ, mn=None, mx=None):
            acc = {
                "bufferView": bv_idx,
                "componentType": comp_type,
                "count": count,
                "type": typ,
            }
            if mn is not None and mx is not None:
                acc["min"] = [round(x, 6) for x in mn]
                acc["max"] = [round(x, 6) for x in mx]
            idx = len(accessors)
            accessors.append(acc)
            return idx

        meshes = []
        nodes = []
        total_verts = 0
        total_tris = 0

        root_children = []
        for idx, p in enumerate(self.parts):
            vcnt = len(p.positions)
            if vcnt == 0 or len(p.indices) == 0:
                continue
            assert vcnt < 65535, f"{self.name}/{p.name} vertex overflow: {vcnt}"
            assert len(p.indices) % 3 == 0, f"{self.name}/{p.name} invalid index count"
            assert 0 <= p.material_idx < len(self.materials), f"Invalid mat index {p.material_idx}"
            total_verts += vcnt
            total_tris += len(p.indices) // 3

            pmin = [min(v[k] for v in p.positions) for k in range(3)]
            pmax = [max(v[k] for v in p.positions) for k in range(3)]
            pos_raw = b"".join(struct.pack("<3f", *v) for v in p.positions)
            nrm_raw = b"".join(struct.pack("<3f", *v) for v in p.normals)
            tan_raw = b"".join(struct.pack("<4f", *v) for v in p.tangents)
            uv_raw = b"".join(struct.pack("<2f", *v) for v in p.uvs)
            idx_raw = b"".join(struct.pack("<H", i) for i in p.indices)

            pos_acc = add_acc(append_bytes(pos_raw, 34962), 5126, vcnt, "VEC3", pmin, pmax)
            nrm_acc = add_acc(append_bytes(nrm_raw, 34962), 5126, vcnt, "VEC3")
            tan_acc = add_acc(append_bytes(tan_raw, 34962), 5126, vcnt, "VEC4")
            uv_acc = add_acc(append_bytes(uv_raw, 34962), 5126, vcnt, "VEC2")
            idx_acc = add_acc(append_bytes(idx_raw, 34963), 5123, len(p.indices), "SCALAR")

            m_idx = len(meshes)
            meshes.append({
                "name": p.name,
                "primitives": [{
                    "attributes": {
                        "POSITION": pos_acc,
                        "NORMAL": nrm_acc,
                        "TANGENT": tan_acc,
                        "TEXCOORD_0": uv_acc,
                    },
                    "indices": idx_acc,
                    "material": p.material_idx,
                }],
            })
            n_idx = len(nodes) + 1
            nodes.append({"name": p.name, "mesh": m_idx})
            root_children.append(n_idx)

        assert total_verts > 0 and total_tris > 0, f"{self.name} produced empty geometry"
        root_node = {"name": self.name, "children": root_children}
        if self.sockets:
            root_node["extras"] = {"sockets": self.sockets}
        all_nodes = [root_node] + nodes

        while len(bin_buf) % 4 != 0:
            bin_buf.append(0)

        gltf = {
            "asset": {"version": "2.0", "generator": "KineticLab3D-PropPipeline-v1.0"},
            "scene": 0,
            "scenes": [{"name": self.name, "nodes": [0]}],
            "nodes": all_nodes,
            "meshes": meshes,
            "materials": self.materials,
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

        return total_verts, total_tris


# ==============================================================================
# 1. Six Virtual Tactical Game Firearms (Real-World-Inspired Proportions)
# Oriented: barrel along -Z, top +Y, grip center near (0, 0, 0)
# ==============================================================================

def build_firearm_glock18():
    b = GlbAssetBuilder("Firearm_Glock18")
    m_poly = b.add_mat("Polymer", (0.13, 0.14, 0.16), metallic=0.05, roughness=0.72)
    m_slide = b.add_mat("NitrideSlide", (0.18, 0.19, 0.22), metallic=0.82, roughness=0.34)
    m_steel = b.add_mat("SteelBarrel", (0.48, 0.50, 0.54), metallic=0.92, roughness=0.26)
    m_sight = b.add_mat("ContrastSight", (0.92, 0.58, 0.16), metallic=0.1, roughness=0.40)

    p_frame = b.part("LowerFrame", m_poly)
    grip_q = q_axis((1, 0, 0), 0.22)
    p_frame.add_chamfered_box(0.0, -0.055, 0.022, 0.0155, 0.064, 0.024, 0.004, rot=grip_q)
    # Dust cover + under-rail
    p_frame.add_chamfered_box(0.0, 0.008, -0.048, 0.0145, 0.013, 0.088, 0.003)
    for rz in (-0.085, -0.105, -0.125):
        p_frame.add_box(0.0, -0.006, rz, 0.011, 0.003, 0.005)
    # Trigger guard + trigger blade
    p_frame.add_box(0.0, -0.032, -0.022, 0.006, 0.004, 0.024)
    p_frame.add_box(0.0, -0.016, -0.044, 0.006, 0.017, 0.004)
    p_frame.add_box(0.0, -0.014, -0.016, 0.0035, 0.013, 0.004, rot=q_axis((1, 0, 0), 0.35))
    # Extended 20-rd magazine baseplate
    p_frame.add_chamfered_box(0.0, -0.128, 0.036, 0.015, 0.012, 0.023, 0.003, rot=grip_q)

    p_slide = b.part("TacticalSlide", m_slide)
    p_slide.add_chamfered_box(0.0, 0.030, -0.052, 0.0155, 0.017, 0.108, 0.004)
    # Rear & front slide serrations
    for sz in (0.032, 0.022, 0.012, -0.125, -0.135):
        p_slide.add_box(0.0, 0.030, sz, 0.0164, 0.013, 0.0025)
    # Fire selector switch
    p_slide.add_cylinder(-0.017, 0.028, 0.046, 0.004, 0.004, 0.004, 8, axis="x")

    p_bbl = b.part("BarrelAndComp", m_steel)
    p_bbl.add_cylinder(0.0, 0.029, -0.165, 0.0075, 0.0075, 0.022, 10, axis="z")
    p_bbl.add_box(0.0, 0.038, -0.038, 0.012, 0.009, 0.020)  # chamber block

    p_sights = b.part("IronSights", m_sight)
    p_sights.add_box(0.0, 0.050, -0.150, 0.003, 0.004, 0.005)
    p_sights.add_box(-0.006, 0.050, 0.042, 0.003, 0.004, 0.005)
    p_sights.add_box(0.006, 0.050, 0.042, 0.003, 0.004, 0.005)

    b.socket("grip", (0.0, -0.03, 0.015))
    b.socket("muzzle", (0.0, 0.029, -0.178))
    return b


def build_firearm_service_pistol():
    b = GlbAssetBuilder("Firearm_ServicePistol")
    m_steel = b.add_mat("ServiceSteel", (0.28, 0.30, 0.34), metallic=0.88, roughness=0.30)
    m_grip = b.add_mat("CheckeredGrip", (0.26, 0.17, 0.11), metallic=0.04, roughness=0.78)
    m_chrome = b.add_mat("ChromeAccent", (0.76, 0.78, 0.80), metallic=0.95, roughness=0.22)

    p_frame = b.part("ServiceFrame", m_steel)
    grip_q = q_axis((1, 0, 0), 0.19)
    p_frame.add_chamfered_box(0.0, -0.052, 0.020, 0.014, 0.062, 0.022, 0.003, rot=grip_q)
    p_frame.add_chamfered_box(0.0, 0.007, -0.045, 0.0135, 0.012, 0.082, 0.003)
    p_frame.add_box(0.0, -0.030, -0.022, 0.0055, 0.004, 0.024)
    p_frame.add_box(0.0, -0.014, -0.044, 0.0055, 0.016, 0.004)
    p_frame.add_box(0.0, 0.015, 0.052, 0.007, 0.006, 0.014)  # beavertail
    p_frame.add_box(0.0, 0.030, -0.055, 0.0145, 0.016, 0.112)

    p_grips = b.part("GripScales", m_grip)
    p_grips.add_chamfered_box(-0.015, -0.052, 0.020, 0.003, 0.052, 0.018, 0.002, rot=grip_q)
    p_grips.add_chamfered_box(0.015, -0.052, 0.020, 0.003, 0.052, 0.018, 0.002, rot=grip_q)

    p_acc = b.part("HammerAndBarrel", m_chrome)
    p_acc.add_cylinder(0.0, 0.029, -0.172, 0.0072, 0.0072, 0.018, 10, axis="z")
    p_acc.add_cylinder(0.0, 0.025, 0.054, 0.007, 0.007, 0.008, 8, axis="x")  # hammer spur
    p_acc.add_box(0.0, -0.013, -0.016, 0.0035, 0.012, 0.005)
    p_acc.add_box(0.0, 0.049, -0.155, 0.003, 0.004, 0.004)
    p_acc.add_box(0.0, 0.049, 0.044, 0.010, 0.004, 0.004)

    b.socket("grip", (0.0, -0.03, 0.015))
    b.socket("muzzle", (0.0, 0.029, -0.183))
    return b


def build_firearm_mp5():
    b = GlbAssetBuilder("Firearm_MP5Style")
    m_rec = b.add_mat("StampedSteel", (0.16, 0.17, 0.19), metallic=0.84, roughness=0.38)
    m_poly = b.add_mat("RibbedPolymer", (0.11, 0.12, 0.13), metallic=0.05, roughness=0.76)
    m_gunmetal = b.add_mat("BarrelMetal", (0.34, 0.36, 0.40), metallic=0.90, roughness=0.28)

    p_rec = b.part("UpperReceiver", m_rec)
    p_rec.add_chamfered_box(0.0, 0.025, -0.055, 0.019, 0.025, 0.165, 0.004)
    p_rec.add_cylinder(0.0, 0.052, -0.145, 0.013, 0.013, 0.195, 10, axis="z")  # cocking tube
    p_rec.add_cylinder(-0.024, 0.054, -0.220, 0.005, 0.005, 0.022, 8, axis="x")  # charging handle
    # Collapsible stock rails + hooded front sight
    p_rec.add_box(-0.018, 0.018, 0.195, 0.004, 0.006, 0.115)
    p_rec.add_box(0.018, 0.018, 0.195, 0.004, 0.006, 0.115)
    p_rec.add_cylinder(0.0, 0.072, -0.245, 0.012, 0.012, 0.012, 10, axis="z")  # front sight hood
    p_rec.add_cylinder(0.0, 0.060, 0.050, 0.010, 0.010, 0.018, 8, axis="y")  # diopter drum

    p_poly = b.part("GripAndHandguard", m_poly)
    grip_q = q_axis((1, 0, 0), 0.24)
    p_poly.add_chamfered_box(0.0, -0.052, 0.012, 0.0155, 0.058, 0.022, 0.004, rot=grip_q)
    p_poly.add_chamfered_box(0.0, -0.005, -0.005, 0.018, 0.019, 0.088, 0.003)
    p_poly.add_chamfered_box(0.0, 0.012, -0.175, 0.022, 0.022, 0.085, 0.006)  # forend handguard
    p_poly.add_chamfered_box(0.0, 0.004, 0.305, 0.018, 0.055, 0.016, 0.004)  # buttpad
    p_poly.add_box(0.0, -0.032, -0.025, 0.005, 0.004, 0.022)  # trigger guard

    p_metal = b.part("BarrelAndCurvedMag", m_gunmetal)
    p_metal.add_cylinder(0.0, 0.025, -0.295, 0.009, 0.0085, 0.115, 10, axis="z")
    # Curved 30-round magazine (3 angled segments)
    for k, (my, mz, ang) in enumerate([(-0.048, -0.068, -0.06), (-0.095, -0.075, -0.16), (-0.140, -0.088, -0.26)]):
        p_metal.add_chamfered_box(0.0, my, mz, 0.012, 0.028, 0.019, 0.002, rot=q_axis((1, 0, 0), ang))

    b.socket("grip", (0.0, -0.03, 0.01))
    b.socket("foregrip", (0.0, -0.01, -0.175))
    b.socket("muzzle", (0.0, 0.025, -0.356))
    return b


def build_firearm_m4a1():
    b = GlbAssetBuilder("Firearm_M4A1Style")
    m_anod = b.add_mat("AnodizedAlum", (0.20, 0.21, 0.23), metallic=0.82, roughness=0.36)
    m_poly = b.add_mat("TacticalPoly", (0.12, 0.13, 0.14), metallic=0.05, roughness=0.74)
    m_steel = b.add_mat("ForgedSteel", (0.32, 0.34, 0.38), metallic=0.92, roughness=0.28)
    m_lens = b.add_mat("OpticGlass", (0.14, 0.34, 0.42), metallic=0.20, roughness=0.08, emissive=(0.12, 0.02, 0.02))

    p_rec = b.part("ReceiverAndRail", m_anod)
    p_rec.add_chamfered_box(0.0, 0.022, -0.038, 0.019, 0.032, 0.125, 0.004)
    p_rec.add_box(0.0, 0.056, -0.040, 0.012, 0.005, 0.120)  # Picatinny upper rail
    p_rec.add_chamfered_box(0.0, 0.025, -0.210, 0.023, 0.024, 0.098, 0.004)  # quad handguard
    for kz in range(5):
        p_rec.add_box(0.0, 0.052, -0.145 - kz * 0.032, 0.013, 0.005, 0.010)
    # Optic red-dot housing
    p_rec.add_chamfered_box(0.0, 0.082, -0.035, 0.018, 0.018, 0.034, 0.004)

    p_lens = b.part("OpticLens", m_lens)
    p_lens.add_cylinder(0.0, 0.082, -0.035, 0.013, 0.013, 0.064, 10, axis="z")

    p_poly = b.part("StockAndGrips", m_poly)
    grip_q = q_axis((1, 0, 0), 0.26)
    p_poly.add_chamfered_box(0.0, -0.048, 0.012, 0.015, 0.056, 0.021, 0.004, rot=grip_q)
    # Vertical foregrip
    p_poly.add_cylinder(0.0, -0.032, -0.205, 0.014, 0.013, 0.072, 10, axis="y")
    # Adjustable carbine stock + buffer tube
    p_poly.add_cylinder(0.0, 0.030, 0.165, 0.015, 0.015, 0.180, 10, axis="z")
    p_poly.add_chamfered_box(0.0, 0.012, 0.235, 0.019, 0.056, 0.082, 0.005)
    # 30-rd STANAG magazine
    p_poly.add_chamfered_box(0.0, -0.058, -0.092, 0.0125, 0.068, 0.030, 0.003, rot=q_axis((1, 0, 0), -0.09))

    p_steel = b.part("BarrelAndFlashHider", m_steel)
    p_steel.add_cylinder(0.0, 0.027, -0.375, 0.0095, 0.009, 0.165, 10, axis="z")
    p_steel.add_cylinder(0.0, 0.027, -0.465, 0.0125, 0.0125, 0.036, 10, axis="z")  # birdcage flash hider
    # Front A-frame sight post
    p_steel.add_box(0.0, 0.054, -0.320, 0.006, 0.022, 0.014)

    b.socket("grip", (0.0, -0.03, 0.01))
    b.socket("foregrip", (0.0, -0.032, -0.205))
    b.socket("muzzle", (0.0, 0.027, -0.486))
    return b


def build_firearm_shotgun():
    b = GlbAssetBuilder("Firearm_PumpShotgun")
    m_steel = b.add_mat("BluedSteel", (0.17, 0.18, 0.21), metallic=0.86, roughness=0.35)
    m_poly = b.add_mat("TacticalPumpPoly", (0.13, 0.14, 0.15), metallic=0.04, roughness=0.78)
    m_shell = b.add_mat("ShellRed", (0.72, 0.14, 0.12), metallic=0.1, roughness=0.55)
    m_brass = b.add_mat("BrassHead", (0.78, 0.62, 0.24), metallic=0.92, roughness=0.25)

    p_rec = b.part("ShotgunReceiver", m_steel)
    p_rec.add_chamfered_box(0.0, 0.022, -0.038, 0.021, 0.032, 0.125, 0.005)
    # Barrel + underbarrel 12G magazine tube
    p_rec.add_cylinder(0.0, 0.036, -0.320, 0.013, 0.012, 0.440, 12, axis="z")
    p_rec.add_cylinder(0.0, 0.008, -0.285, 0.012, 0.012, 0.370, 10, axis="z")
    p_rec.add_box(0.0, 0.022, -0.450, 0.006, 0.016, 0.014)  # barrel clamp
    p_rec.add_box(0.0, 0.055, -0.525, 0.004, 0.006, 0.006)  # bead sight

    p_poly = b.part("StockAndPump", m_poly)
    # Angled semi-pistol grip & shoulder stock
    stock_q = q_axis((1, 0, 0), 0.16)
    p_poly.add_chamfered_box(0.0, -0.012, 0.185, 0.018, 0.048, 0.145, 0.006, rot=stock_q)
    p_poly.add_chamfered_box(0.0, -0.045, 0.020, 0.016, 0.042, 0.022, 0.004, rot=q_axis((1, 0, 0), 0.32))
    # Ribbed pump forend
    p_poly.add_cylinder(0.0, 0.008, -0.245, 0.024, 0.022, 0.165, 12, axis="z")
    for k in range(5):
        p_poly.add_torus(0.0, 0.008, -0.185 - k * 0.028, 0.024, 0.003, 10, 6)

    # Side-saddle 4 shell holder
    p_shells = b.part("SideSaddleShells", m_shell)
    p_brass = b.part("SideSaddleBrass", m_brass)
    for k in range(4):
        sz = -0.010 - k * 0.024
        p_shells.add_cylinder(0.024, 0.022, sz, 0.0085, 0.0085, 0.042, 8, axis="y")
        p_brass.add_cylinder(0.024, -0.002, sz, 0.0088, 0.0088, 0.010, 8, axis="y")

    b.socket("grip", (0.0, -0.028, 0.015))
    b.socket("foregrip", (0.0, 0.0, -0.245))
    b.socket("muzzle", (0.0, 0.036, -0.542))
    return b


def build_firearm_sniper():
    b = GlbAssetBuilder("Firearm_BoltSniper")
    m_chassis = b.add_mat("OliveChassis", (0.24, 0.27, 0.22), metallic=0.45, roughness=0.50)
    m_barrel = b.add_mat("FlutedSteel", (0.18, 0.19, 0.21), metallic=0.90, roughness=0.28)
    m_poly = b.add_mat("MatteGripPoly", (0.11, 0.12, 0.13), metallic=0.05, roughness=0.76)
    m_optic = b.add_mat("ScopeGlass", (0.10, 0.28, 0.44), metallic=0.25, roughness=0.06, emissive=(0.02, 0.08, 0.14))

    p_ch = b.part("PrecisionChassis", m_chassis)
    p_ch.add_chamfered_box(0.0, 0.016, -0.095, 0.022, 0.030, 0.220, 0.004)
    # Adjustable cheek riser + skeleton stock
    p_ch.add_chamfered_box(0.0, 0.016, 0.205, 0.017, 0.038, 0.125, 0.004)
    p_ch.add_chamfered_box(0.0, 0.056, 0.200, 0.015, 0.012, 0.075, 0.004)

    p_bbl = b.part("BarrelScopeBipod", m_barrel)
    # Heavy free-float barrel + tank muzzle brake
    p_bbl.add_cylinder(0.0, 0.038, -0.375, 0.014, 0.011, 0.500, 12, axis="z")
    p_bbl.add_chamfered_box(0.0, 0.038, -0.642, 0.018, 0.014, 0.035, 0.003)
    # Bolt handle knob
    p_bbl.add_box(0.026, 0.032, 0.015, 0.018, 0.004, 0.004)
    p_bbl.add_sphere(0.044, 0.024, 0.015, 0.009, 6, 8)
    # Long-range variable scope + mounts + turrets
    p_bbl.add_cylinder(0.0, 0.088, -0.055, 0.016, 0.016, 0.260, 12, axis="z")
    p_bbl.add_cylinder(0.0, 0.088, -0.175, 0.024, 0.026, 0.075, 12, axis="z")  # objective bell
    p_bbl.add_cylinder(0.0, 0.088, 0.065, 0.021, 0.021, 0.050, 12, axis="z")  # ocular bell
    p_bbl.add_box(0.0, 0.062, -0.095, 0.014, 0.018, 0.014)
    p_bbl.add_box(0.0, 0.062, -0.005, 0.014, 0.018, 0.014)
    p_bbl.add_cylinder(0.0, 0.110, -0.055, 0.008, 0.008, 0.014, 8, axis="y")  # elevation turret
    # Folded bipod legs under forend
    p_bbl.add_cylinder(-0.026, -0.006, -0.255, 0.006, 0.006, 0.135, 8, axis="z")
    p_bbl.add_cylinder(0.026, -0.006, -0.255, 0.006, 0.006, 0.135, 8, axis="z")

    p_poly = b.part("GripMagButt", m_poly)
    p_poly.add_chamfered_box(0.0, -0.052, 0.022, 0.015, 0.058, 0.022, 0.004, rot=q_axis((1, 0, 0), 0.18))
    p_poly.add_chamfered_box(0.0, -0.036, -0.068, 0.013, 0.038, 0.035, 0.003)  # 5rd match mag
    p_poly.add_chamfered_box(0.0, 0.014, 0.335, 0.018, 0.052, 0.014, 0.004)  # rubber buttpad

    p_opt = b.part("ScopeLenses", m_optic)
    p_opt.add_cylinder(0.0, 0.088, -0.212, 0.022, 0.022, 0.004, 12, axis="z")
    p_opt.add_cylinder(0.0, 0.088, 0.090, 0.018, 0.018, 0.004, 12, axis="z")

    b.socket("grip", (0.0, -0.03, 0.018))
    b.socket("foregrip", (0.0, 0.0, -0.220))
    b.socket("muzzle", (0.0, 0.038, -0.680))
    return b


# ==============================================================================
# 2. Six Collectible Caliber Ammunition Boxes (Unit Props matching ObjectType scale)
# ==============================================================================

def build_ammo_box(name, hx, hy, hz, shell_color, stripe_color, caliber_kind):
    b = GlbAssetBuilder(name)
    m_shell = b.add_mat("AmmoShell", shell_color, metallic=0.35, roughness=0.58)
    m_latch = b.add_mat("SteelLatch", (0.64, 0.67, 0.72), metallic=0.90, roughness=0.28)
    m_band = b.add_mat("CaliberStripe", stripe_color, metallic=0.12, roughness=0.42)
    m_brass = b.add_mat("CartridgeTrim", (0.80, 0.64, 0.24), metallic=0.88, roughness=0.24)

    p_body = b.part("AmmoCrateBody", m_shell)
    p_body.add_chamfered_box(0.0, -hy * 0.14, 0.0, hx * 0.94, hy * 0.72, hz * 0.92, 0.012)
    # Recessed protective corner bumpers
    for sx in (-1, 1):
        for sz in (-1, 1):
            p_body.add_box(sx * hx * 0.90, -hy * 0.14, sz * hz * 0.88, hx * 0.08, hy * 0.72, hz * 0.08)
    # Hinged overlapping lid
    p_body.add_chamfered_box(0.0, hy * 0.72, 0.0, hx * 0.98, hy * 0.20, hz * 0.97, 0.010)

    p_band = b.part("CaliberIdentifierBand", m_band)
    p_band.add_box(0.0, hy * 0.18, hz * 0.93, hx * 0.84, hy * 0.18, 0.008)
    p_band.add_box(0.0, hy * 0.18, -hz * 0.93, hx * 0.84, hy * 0.18, 0.008)
    p_band.add_box(hx * 0.95, hy * 0.18, 0.0, 0.008, hy * 0.18, hz * 0.78)
    p_band.add_box(-hx * 0.95, hy * 0.18, 0.0, 0.008, hy * 0.18, hz * 0.78)

    p_latch = b.part("HardwareLatchesHandle", m_latch)
    # Top folding carry handle + side draw latches
    p_latch.add_box(-hx * 0.30, hy * 0.95, 0.0, 0.012, 0.015, 0.012)
    p_latch.add_box(hx * 0.30, hy * 0.95, 0.0, 0.012, 0.015, 0.012)
    p_latch.add_chamfered_box(0.0, hy * 1.04, 0.0, hx * 0.34, 0.012, 0.014, 0.004)
    p_latch.add_box(0.0, hy * 0.45, hz * 0.97, 0.032, 0.036, 0.010)
    p_latch.add_box(0.0, hy * 0.45, -hz * 0.97, 0.032, 0.036, 0.010)

    p_trim = b.part("CartridgeSymbol", m_brass)
    count = 3 if caliber_kind == "rifle" else (2 if caliber_kind == "shotgun" else 4)
    for k in range(count):
        ox = (k - (count - 1) * 0.5) * (hx * 0.32)
        p_trim.add_cylinder(ox, hy * 0.18, hz * 0.945, 0.010, 0.006, 0.042, 8, axis="y")
    return b


# ==============================================================================
# 3. 34 Industrial & Lab Tool GLBs (Matching +Y ToolMeshStyle Axes)
# ==============================================================================

def build_tool_glb(style_name):
    b = GlbAssetBuilder(f"Tool_{style_name}")
    m_steel = b.add_mat("ToolSteel", (0.58, 0.62, 0.66), metallic=0.88, roughness=0.28)
    m_dark = b.add_mat("GripDark", (0.16, 0.18, 0.20), metallic=0.08, roughness=0.72)
    m_accent = b.add_mat("IndustrialOrange", (0.86, 0.46, 0.14), metallic=0.12, roughness=0.45)
    m_brass = b.add_mat("BrassCopper", (0.80, 0.56, 0.22), metallic=0.86, roughness=0.30)
    m_glass = b.add_mat("LensGlass", (0.52, 0.78, 0.86), metallic=0.1, roughness=0.10, alpha=0.85)

    p_s = b.part("MetalBody", m_steel)
    p_g = b.part("ErgonomicGrip", m_dark)
    p_a = b.part("HousingAccent", m_accent)

    if style_name == "HAMMER_CLAW":
        p_g.add_cylinder(0, -0.05, 0, 0.030, 0.036, 0.54, 12)
        p_a.add_cylinder(0, -0.18, 0, 0.035, 0.033, 0.24, 12)
        p_s.add_chamfered_box(0, 0.24, 0.02, 0.046, 0.044, 0.16, 0.010)
        p_s.add_cylinder(0, 0.24, 0.19, 0.042, 0.042, 0.04, 12, axis="z")
        p_s.add_box(-0.02, 0.23, -0.17, 0.014, 0.022, 0.07)
        p_s.add_box(0.02, 0.23, -0.17, 0.014, 0.022, 0.07)
    elif style_name == "MALLET_HEAD":
        p_g.add_cylinder(0, -0.06, 0, 0.030, 0.034, 0.54, 12)
        p_s.add_cylinder(0, 0.22, 0, 0.068, 0.068, 0.24, 14, axis="z")
    elif style_name == "WRENCH_OPEN":
        p_s.add_chamfered_box(0, 0, 0, 0.038, 0.24, 0.016, 0.005)
        p_s.add_chamfered_box(-0.042, 0.24, 0, 0.030, 0.06, 0.020, 0.005)
        p_s.add_chamfered_box(0.042, 0.24, 0, 0.030, 0.06, 0.020, 0.005)
        p_s.add_torus(0, -0.24, 0, 0.055, 0.016, 14, 6, vertical_x=True)
    elif style_name == "RATCHET_SOCKET":
        p_s.add_cylinder(0, -0.04, 0, 0.024, 0.024, 0.42, 10)
        p_g.add_cylinder(0, -0.18, 0, 0.034, 0.032, 0.18, 12)
        p_s.add_cylinder(0, 0.20, 0, 0.064, 0.064, 0.055, 14)
        p_s.add_cylinder(0, 0.24, 0.04, 0.034, 0.034, 0.08, 12)
    elif style_name == "SCREWDRIVER_DRIVER":
        p_a.add_cylinder(0, -0.14, 0, 0.042, 0.038, 0.22, 12)
        p_g.add_cylinder(0, -0.14, 0, 0.044, 0.040, 0.12, 8)
        p_s.add_cylinder(0, 0.14, 0, 0.012, 0.011, 0.34, 8)
        p_s.add_box(0, 0.32, 0, 0.018, 0.028, 0.005)
    elif style_name == "PLIERS_JAWS":
        p_a.add_box(-0.034, -0.12, 0, 0.017, 0.15, 0.018)
        p_a.add_box(0.034, -0.12, 0, 0.017, 0.15, 0.018)
        p_s.add_cylinder(0, 0.05, 0, 0.044, 0.044, 0.036, 12, axis="z")
        p_s.add_box(-0.016, 0.16, 0, 0.014, 0.10, 0.016)
        p_s.add_box(0.016, 0.16, 0, 0.014, 0.10, 0.016)
    elif style_name == "HAND_SAW_BLADE":
        p_a.add_chamfered_box(0, -0.18, 0, 0.075, 0.055, 0.030, 0.008)
        p_s.add_box(0, 0.08, 0, 0.085, 0.25, 0.007)
        for i in range(7):
            p_s.add_box(0.085, -0.10 + i * 0.065, 0, 0.018, 0.014, 0.007)
    elif style_name == "POWER_DRILL":
        p_g.add_chamfered_box(0, -0.10, -0.04, 0.040, 0.11, 0.048, 0.010)
        p_g.add_chamfered_box(0, -0.22, -0.04, 0.062, 0.040, 0.075, 0.010)
        p_a.add_chamfered_box(0, 0.07, 0.02, 0.054, 0.064, 0.16, 0.014)
        p_s.add_cylinder(0, 0.07, 0.21, 0.038, 0.032, 0.06, 12, axis="z")
        p_s.add_cylinder(0, 0.07, 0.30, 0.010, 0.010, 0.12, 8, axis="z")
    elif style_name == "ANGLE_GRINDER":
        p_a.add_chamfered_box(0, 0, -0.08, 0.048, 0.048, 0.19, 0.014)
        p_g.add_cylinder(0.09, -0.01, 0.08, 0.018, 0.016, 0.12, 8, axis="x")
        p_s.add_cylinder(0, 0.02, 0.14, 0.062, 0.062, 0.075, 12)
        p_s.add_cylinder(0, -0.04, 0.16, 0.15, 0.15, 0.014, 18)
    elif style_name == "CIRCULAR_SAW":
        p_s.add_box(0, -0.08, 0, 0.13, 0.016, 0.21)
        p_a.add_chamfered_box(-0.04, 0.06, 0, 0.075, 0.085, 0.13, 0.016)
        p_g.add_chamfered_box(-0.04, 0.16, -0.04, 0.028, 0.036, 0.075, 0.008)
        p_s.add_cylinder(0.06, -0.02, 0, 0.15, 0.15, 0.014, 18, axis="x")
    elif style_name == "HEAT_GUN_NOZZLE":
        p_g.add_chamfered_box(0, -0.10, -0.04, 0.038, 0.10, 0.044, 0.008)
        p_a.add_chamfered_box(0, 0.06, 0, 0.052, 0.052, 0.14, 0.014)
        p_s.add_cylinder(0, 0.06, 0.21, 0.034, 0.022, 0.14, 12, axis="z")
    elif style_name in ("WELDING_UNIT", "GENERATOR_FRAME"):
        p_a.add_chamfered_box(0, 0, 0, 0.19, 0.22, 0.26, 0.022)
        p_g.add_box(0, 0.24, 0, 0.028, 0.028, 0.15)
        p_s.add_box(0.20, 0.06, 0.10, 0.016, 0.08, 0.08)
    elif style_name == "WELDING_TORCH":
        p_g.add_cylinder(0, -0.06, 0, 0.030, 0.030, 0.25, 10)
        p_b = b.part("BrassNozzle", m_brass)
        p_b.add_cylinder(0, 0.14, 0.04, 0.016, 0.012, 0.20, 10)
        p_b.add_cylinder(0, 0.24, 0.08, 0.022, 0.016, 0.05, 10)
    elif style_name == "CLAMP_C_FRAME":
        p_a.add_box(-0.10, 0, 0, 0.028, 0.19, 0.028)
        p_a.add_box(0, 0.17, 0, 0.11, 0.028, 0.028)
        p_a.add_box(0, -0.17, 0, 0.11, 0.028, 0.028)
        p_s.add_cylinder(0.10, -0.05, 0, 0.015, 0.015, 0.25, 8)
    elif style_name == "HYDRAULIC_JACK":
        p_a.add_chamfered_box(0, -0.17, 0, 0.13, 0.038, 0.13, 0.010)
        p_a.add_cylinder(0, -0.02, 0, 0.075, 0.075, 0.25, 14)
        p_s.add_cylinder(0, 0.16, 0, 0.042, 0.042, 0.14, 12)
    elif style_name == "HOIST_WINCH":
        p_a.add_chamfered_box(0, 0, 0, 0.15, 0.13, 0.15, 0.018)
        p_s.add_cylinder(0, 0, 0, 0.095, 0.095, 0.21, 14, axis="x")
    elif style_name == "COMPRESSOR_TANK":
        p_a.add_cylinder(0, -0.02, 0, 0.15, 0.15, 0.34, 16)
        p_s.add_chamfered_box(0, 0.18, 0, 0.09, 0.05, 0.11, 0.012)
        p_g.add_box(0, -0.20, 0, 0.17, 0.02, 0.15)
    elif style_name == "FAN_SHROUD":
        p_a.add_torus(0, 0, 0, 0.21, 0.030, 18, 8, vertical_x=True)
        p_s.add_cylinder(0, 0, 0, 0.055, 0.055, 0.09, 12, axis="z")
        for i in range(5):
            a = (i / 5.0) * TAU
            p_s.add_box(math.cos(a) * 0.11, math.sin(a) * 0.11, 0, 0.03, 0.075, 0.012, rot=q_axis((0, 0, 1), a))
    elif style_name == "DIGITAL_METER":
        p_a.add_chamfered_box(0, 0, 0, 0.095, 0.17, 0.036, 0.010)
        p_g.add_box(0, 0.08, 0.038, 0.068, 0.052, 0.006)
        p_s.add_cylinder(0, -0.04, 0.038, 0.040, 0.040, 0.016, 12, axis="z")
    elif style_name == "THERMOMETER_PROBE":
        p_g.add_chamfered_box(0, -0.06, 0, 0.038, 0.10, 0.038, 0.008)
        p_a.add_chamfered_box(0, 0.08, 0.045, 0.042, 0.048, 0.10, 0.008)
    elif style_name == "SCALE_PLATFORM":
        p_g.add_chamfered_box(0, -0.04, 0, 0.21, 0.038, 0.21, 0.012)
        p_s.add_chamfered_box(0, 0.02, 0, 0.17, 0.018, 0.17, 0.010)
    elif style_name == "CALIPER_SLIDE":
        p_s.add_box(0, 0, 0, 0.018, 0.23, 0.010)
        p_s.add_box(0.075, 0.17, 0, 0.075, 0.022, 0.012)
        p_s.add_box(0.075, 0.02, 0, 0.075, 0.022, 0.012)
    elif style_name == "PRESSURE_DIAL":
        p_b = b.part("BrassBody", m_brass)
        p_b.add_cylinder(0, 0.04, 0, 0.115, 0.115, 0.048, 16, axis="z")
        p_b.add_cylinder(0, -0.10, 0, 0.028, 0.028, 0.15, 10)
    elif style_name == "OPTICAL_MICROSCOPE":
        p_g.add_chamfered_box(0, -0.20, 0, 0.13, 0.032, 0.17, 0.010)
        p_s.add_box(0, -0.02, -0.09, 0.032, 0.17, 0.038)
        p_s.add_cylinder(0, 0.13, 0.03, 0.030, 0.024, 0.19, 12)
    elif style_name == "LAB_BEAKER":
        p_gl = b.part("Borosilicate", m_glass)
        p_gl.add_cylinder(0, 0, 0, 0.115, 0.125, 0.27, 16)
        p_s.add_torus(0, 0.135, 0, 0.125, 0.012, 16, 6)
    elif style_name == "EXTINGUISHER_CYL":
        m_red = b.add_mat("SafetyRed", (0.76, 0.14, 0.12), metallic=0.25, roughness=0.40)
        p_r = b.part("TankRed", m_red)
        p_r.add_cylinder(0, -0.02, 0, 0.098, 0.098, 0.35, 16)
        p_s.add_cylinder(0, 0.20, 0, 0.030, 0.030, 0.08, 10)
        p_g.add_cylinder(-0.10, 0.02, 0.02, 0.018, 0.032, 0.17, 10)
    elif style_name == "FLASHLIGHT_TORCH":
        p_g.add_cylinder(0, -0.04, 0, 0.034, 0.034, 0.27, 14)
        p_s.add_cylinder(0, 0.14, 0, 0.040, 0.062, 0.10, 14)
        m_lit = b.add_mat("BeamLens", (0.98, 0.94, 0.80), 0.1, 0.1, 1.0, (0.85, 0.78, 0.55))
        p_l = b.part("EmitterLens", m_lit)
        p_l.add_cylinder(0, 0.192, 0, 0.056, 0.056, 0.010, 12)
    elif style_name == "WORKLIGHT_TRIPOD":
        p_g.add_box(0, -0.14, 0, 0.15, 0.018, 0.13)
        p_s.add_cylinder(0, -0.02, 0, 0.022, 0.022, 0.22, 8)
        p_a.add_chamfered_box(0, 0.14, 0, 0.14, 0.10, 0.055, 0.012)
    elif style_name == "DETECTOR_WAND":
        p_s.add_cylinder(0, 0.06, 0, 0.020, 0.020, 0.40, 10)
        p_a.add_chamfered_box(0, 0.21, 0, 0.050, 0.042, 0.065, 0.010)
        p_g.add_torus(0, -0.18, 0, 0.13, 0.020, 16, 6)
    elif style_name == "MAGNET_HORSESHOE":
        m_red = b.add_mat("MagnetRed", (0.78, 0.16, 0.14), metallic=0.65, roughness=0.36)
        p_r = b.part("MagnetArms", m_red)
        p_r.add_box(-0.095, 0.04, 0, 0.036, 0.13, 0.036)
        p_r.add_box(0.095, 0.04, 0, 0.036, 0.13, 0.036)
        p_r.add_box(0, -0.095, 0, 0.13, 0.038, 0.036)
        p_s.add_box(-0.095, 0.185, 0, 0.036, 0.025, 0.036)
        p_s.add_box(0.095, 0.185, 0, 0.036, 0.025, 0.036)
    elif style_name == "SUCTION_CUP_HANDLE":
        p_g.add_cylinder(0, -0.06, 0, 0.13, 0.048, 0.075, 16)
        p_a.add_box(0, 0.04, 0, 0.032, 0.075, 0.032)
        p_a.add_box(0, 0.10, 0, 0.10, 0.025, 0.028)
    elif style_name == "PULLEY_BLOCK":
        p_s.add_box(-0.052, 0, 0, 0.014, 0.15, 0.075)
        p_s.add_box(0.052, 0, 0, 0.014, 0.15, 0.075)
        p_a.add_torus(0, 0, 0, 0.095, 0.022, 16, 6, vertical_x=True)
    else:  # SPIRIT_LEVEL
        p_a.add_chamfered_box(0, 0, 0, 0.30, 0.042, 0.024, 0.008)
        p_s.add_cylinder(0, 0.044, 0, 0.020, 0.020, 0.014, 10)
    return b


# ==============================================================================
# 4. 53 Prop & Architectural Mesh GLBs (Matching MeshId Unit Bounds)
# ==============================================================================

def build_prop_glb(mesh_id):
    b = GlbAssetBuilder(f"Prop_{mesh_id}")
    m_base = b.add_mat("PrimarySurface", (0.76, 0.78, 0.80), metallic=0.25, roughness=0.55)
    m_steel = b.add_mat("TrimSteel", (0.48, 0.52, 0.56), metallic=0.84, roughness=0.32)
    m_accent = b.add_mat("SafetyTrim", (0.86, 0.50, 0.16), metallic=0.18, roughness=0.48)
    m_dark = b.add_mat("DarkRubber", (0.15, 0.16, 0.18), metallic=0.05, roughness=0.78)
    m_glass = b.add_mat("Glazing", (0.70, 0.84, 0.90), metallic=0.08, roughness=0.10, alpha=0.45)

    p = b.part("Body", m_base)
    p_s = b.part("HardwareTrim", m_steel)

    if mesh_id == "REINFORCED_CRATE":
        hx = hy = hz = 0.55
        p.add_box(0, 0, 0, hx * 0.90, hy * 0.90, hz * 0.90)
        t = hx * 0.14
        for sx in (-1, 1):
            for sy in (-1, 1):
                p_s.add_chamfered_box(sx * (hx - t * 0.7), sy * (hy - t * 0.7), 0, t, t, hz, 0.018)
        for sy in (-1, 1):
            for sz in (-1, 1):
                p_s.add_chamfered_box(0, sy * (hy - t * 0.7), sz * (hz - t * 0.7), hx, t, t, 0.018)
        for sx in (-1, 1):
            for sz in (-1, 1):
                p_s.add_chamfered_box(sx * (hx - t * 0.7), 0, sz * (hz - t * 0.7), t, hy, t, 0.018)
    elif mesh_id == "WOOD_PALLET_CRATE":
        hx = hy = hz = 0.52
        p.add_box(0, 0, 0, hx * 0.88, hy * 0.88, hz * 0.88)
        for sx in (-1, 1):
            for sz in (-1, 1):
                p_s.add_box(sx * (hx - 0.07), 0, sz * (hz - 0.07), 0.07, hy, 0.07)
        for y in (-0.34, 0.0, 0.34):
            p_s.add_box(0, y, hz - 0.025, hx, 0.055, 0.025)
            p_s.add_box(0, y, -hz + 0.025, hx, 0.055, 0.025)
    elif mesh_id == "TREADED_TIRE":
        p.add_torus(0, 0, 0, 0.36, 0.16, 20, 10)
        p_s.add_cylinder(0, 0, 0, 0.22, 0.22, 0.22, 12)
        for i in range(14):
            a = (i / 14.0) * TAU
            p.add_box(math.cos(a) * 0.51, 0, math.sin(a) * 0.51, 0.025, 0.10, 0.025)
    elif mesh_id == "GEODESIC_SPHERE":
        p.add_sphere(0, 0, 0, 0.44, 14, 20)
        p_s.add_torus(0, 0, 0, 0.43, 0.022, 16, 6)
    elif mesh_id == "GYRO_FLYWHEEL":
        p.add_torus(0, 0, 0, 0.48, 0.042, 18, 6, vertical_x=True)
        p_s.add_torus(0, 0, 0, 0.38, 0.036, 18, 6, vertical_x=False)
        p_s.add_cylinder(0, 0, 0, 0.04, 0.04, 0.86, 10)
        p.add_cylinder(0, 0, 0, 0.23, 0.23, 0.11, 16)
    elif mesh_id == "STEEL_IBEAM":
        p.add_chamfered_box(0, 0.17, 0, 1.50, 0.05, 0.24, 0.012)
        p.add_chamfered_box(0, -0.17, 0, 1.50, 0.05, 0.24, 0.012)
        p.add_box(0, 0, 0, 1.48, 0.14, 0.055)
        for x in (-1.45, 0.0, 1.45):
            p_s.add_box(x, 0, 0, 0.035, 0.14, 0.22)
    elif mesh_id == "GLASS_CONTAINER":
        p_gl = b.part("GlassShell", m_glass)
        p_gl.add_chamfered_box(0, 0, 0, 0.50, 0.50, 0.50, 0.042)
        p_s.add_box(0, 0.48, 0, 0.50, 0.02, 0.50)
    elif mesh_id == "CONCRETE_MASONRY":
        p.add_chamfered_box(0, 0, 0, 0.60, 0.35, 0.35, 0.032)
        p_s.add_box(-0.26, 0.36, 0, 0.09, 0.02, 0.15)
        p_s.add_box(0.26, 0.36, 0, 0.09, 0.02, 0.15)
    elif mesh_id == "DOMINO_MONOLITH":
        p.add_chamfered_box(0, 0, 0, 0.14, 0.90, 0.45, 0.022)
        p_s.add_box(0, 0, 0, 0.152, 0.025, 0.38)
    elif mesh_id == "RAMP_WEDGE":
        p.add_chamfered_box(0, 0.10, 0, 0.94, 0.90, 1.0, 0.04)
        p_s.add_box(-0.95, 0.35, 0, 0.05, 0.65, 1.0)
        p_s.add_box(0.95, 0.35, 0, 0.05, 0.65, 1.0)
    elif mesh_id == "CENTRIFUGE_POD":
        p_s.add_cylinder(0, -0.45, 0, 0.44, 0.42, 0.22, 16)
        p.add_cylinder(0, 0, 0, 0.38, 0.34, 0.72, 16)
        p_s.add_cylinder(0, 0.46, 0, 0.36, 0.26, 0.22, 16)
    elif mesh_id == "SPRING_PISTON":
        p_s.add_cylinder(0, -0.56, 0, 0.34, 0.34, 0.14, 14)
        p_s.add_cylinder(0, 0.56, 0, 0.34, 0.34, 0.14, 14)
        p.add_coil(0, 0, 0, 0.22, 0.042, 0.98, 5.0, 32)
    elif mesh_id == "BARREL_DRUM":
        p.add_cylinder(0, 0, 0, 0.37, 0.37, 1.14, 18)
        p_s.add_torus(0, 0.28, 0, 0.365, 0.020, 16, 6)
        p_s.add_torus(0, -0.28, 0, 0.365, 0.020, 16, 6)
    elif mesh_id in ("FLOOR_SLAB", "WALL_PANEL", "PILLAR_COLUMN"):
        bevel = 0.015 if mesh_id == "FLOOR_SLAB" else (0.020 if mesh_id == "WALL_PANEL" else 0.04)
        p.add_chamfered_box(0, 0, 0, 1.0, 1.0, 1.0, bevel)
    elif mesh_id == "GLASS_PARTITION":
        p_gl = b.part("GlassPane", m_glass)
        p_gl.add_box(0, 0, 0, 0.98, 0.94, 0.70)
        p_s.add_box(0, 0.97, 0, 1.0, 0.03, 1.0)
        p_s.add_box(0, -0.97, 0, 1.0, 0.03, 1.0)
        p_s.add_box(-0.97, 0, 0, 0.03, 1.0, 1.0)
        p_s.add_box(0.97, 0, 0, 0.03, 1.0, 1.0)
    elif mesh_id == "CRANE_BOOM":
        p.add_box(0, 0.85, 0.85, 1.0, 0.12, 0.12)
        p.add_box(0, 0.85, -0.85, 1.0, 0.12, 0.12)
        p.add_box(0, -0.85, 0, 1.0, 0.14, 0.16)
        for i in range(-4, 5):
            p_s.add_box(i * 0.22, 0, 0, 0.03, 0.85, 0.85)
    elif mesh_id == "LAB_CONSOLE":
        p.add_chamfered_box(0, -0.10, 0, 0.98, 0.88, 0.96, 0.03)
        p_s.add_chamfered_box(0, 0.90, 0, 1.0, 0.10, 1.0, 0.02)
        for sx in (-0.46, 0.46):
            p_s.add_chamfered_box(sx, 0.34, -0.965, 0.38, 0.22, 0.025, 0.01)
            p_s.add_chamfered_box(sx, -0.32, -0.965, 0.38, 0.34, 0.025, 0.01)
        p_d = b.part("DialPad", m_dark)
        p_d.add_box(0, 0.45, 0.97, 0.88, 0.34, 0.03)
    elif mesh_id == "SHELF_RACK":
        for sx in (-0.93, 0.93):
            for sz in (-0.93, 0.93):
                p_s.add_box(sx, 0, sz, 0.07, 1.0, 0.07)
        for y in (-0.88, -0.18, 0.48, 0.95):
            p.add_box(0, y, 0, 0.98, 0.05, 0.98)
    elif mesh_id == "CONVEYOR_UNIT":
        p_s.add_box(0, 0.72, 0.93, 1.0, 0.24, 0.07)
        p_s.add_box(0, 0.72, -0.93, 1.0, 0.24, 0.07)
        p_d = b.part("BeltDeck", m_dark)
        p_d.add_box(0, 0.80, 0, 0.98, 0.20, 0.86)
        for x in (-0.86, -0.43, 0.0, 0.43, 0.86):
            p.add_box(x, -0.16, 0.88, 0.045, 0.84, 0.06)
            p.add_box(x, -0.16, -0.88, 0.045, 0.84, 0.06)
    elif mesh_id == "SHIPPING_CONTAINER":
        p.add_chamfered_box(0, 0, 0, 0.97, 1.0, 0.98, 0.03)
        for i in range(-4, 5):
            t = i * 0.21
            p_s.add_box(0.985, 0, t, 0.015, 0.92, 0.04)
            p_s.add_box(-0.985, 0, t, 0.015, 0.92, 0.04)
    elif mesh_id == "SCAFFOLD_FRAME":
        for sx in (-0.95, 0.95):
            for sz in (-0.95, 0.95):
                p_s.add_box(sx, 0, sz, 0.05, 1.0, 0.05)
        for y in (-0.65, 0.05, 0.65, 0.96):
            p.add_box(0, y, 0.95, 0.95, 0.035, 0.035)
            p.add_box(0, y, -0.95, 0.95, 0.035, 0.035)
            p.add_box(0.95, y, 0, 0.035, 0.035, 0.95)
            p.add_box(-0.95, y, 0, 0.035, 0.035, 0.95)
    elif mesh_id == "BATTERY_UNIT":
        p.add_chamfered_box(0, -0.02, 0, 0.32, 0.24, 0.24, 0.024)
        p_s.add_cylinder(-0.16, 0.24, 0, 0.04, 0.036, 0.08, 10)
        p_s.add_cylinder(0.16, 0.24, 0, 0.04, 0.036, 0.08, 10)
    elif mesh_id == "SWITCH_BOX":
        p.add_chamfered_box(0, -0.04, 0, 0.24, 0.11, 0.24, 0.02)
        p_s.add_cylinder(0, 0.08, 0, 0.052, 0.052, 0.04, 10)
        p_s.add_box(0, 0.13, 0.04, 0.022, 0.07, 0.022)
    elif mesh_id == "LIGHT_BULB_LAMP":
        p_s.add_cylinder(0, -0.20, 0, 0.10, 0.11, 0.18, 12)
        p.add_sphere(0, 0.05, 0, 0.21, 10, 14)
    elif mesh_id == "LED_INDICATOR":
        p_s.add_chamfered_box(0, -0.14, 0, 0.16, 0.06, 0.16, 0.015)
        p.add_cylinder(0, 0.02, 0, 0.10, 0.09, 0.20, 12)
        p.add_sphere(0, 0.12, 0, 0.095, 8, 12)
    elif mesh_id == "DC_MOTOR":
        p_s.add_box(0, -0.26, 0, 0.34, 0.05, 0.32)
        p.add_cylinder(0, 0.02, 0, 0.26, 0.26, 0.50, 16)
        p_s.add_cylinder(0, 0.32, 0, 0.055, 0.055, 0.16, 10)
    elif mesh_id == "KINETIC_GENERATOR":
        p_s.add_chamfered_box(0, -0.28, 0, 0.40, 0.06, 0.40, 0.02)
        p.add_cylinder(0, 0.02, 0, 0.30, 0.30, 0.48, 16)
        p_s.add_torus(0, 0.30, 0, 0.24, 0.04, 14, 6)
    elif mesh_id == "FUSE_BLOCK":
        p.add_chamfered_box(0, -0.06, 0, 0.26, 0.06, 0.18, 0.015)
        p_s.add_cylinder(0, 0.05, 0, 0.055, 0.055, 0.18, 10)
    elif mesh_id == "CIRCUIT_CONNECTOR":
        p.add_chamfered_box(0, -0.04, 0, 0.20, 0.08, 0.20, 0.015)
        p_s.add_cylinder(-0.08, 0.08, 0, 0.038, 0.038, 0.10, 8)
        p_s.add_cylinder(0.08, 0.08, 0, 0.038, 0.038, 0.10, 8)
    elif mesh_id == "ELECTRICAL_PANEL":
        p.add_chamfered_box(0, 0, 0, 0.46, 0.60, 0.18, 0.025)
        p_s.add_cylinder(-0.20, 0.28, 0.19, 0.10, 0.10, 0.03, 12, axis="z")
        p_s.add_cylinder(0.20, 0.28, 0.19, 0.10, 0.10, 0.03, 12, axis="z")
        p_s.add_box(0, 0.02, 0.19, 0.34, 0.09, 0.022)
    elif mesh_id == "LAB_WORKBENCH":
        p.add_chamfered_box(0, 0.40, 0, 1.10, 0.08, 0.55, 0.02)
        for sx in (-0.98, 0.98):
            for sz in (-0.45, 0.45):
                p_s.add_box(sx, -0.04, sz, 0.06, 0.44, 0.06)
        p_s.add_box(0, -0.22, 0, 0.96, 0.04, 0.42)
    elif mesh_id == "OFFICE_CHAIR":
        p.add_chamfered_box(0, -0.04, 0, 0.32, 0.06, 0.32, 0.02)
        p.add_chamfered_box(0, 0.28, -0.28, 0.28, 0.26, 0.05, 0.02)
        p_s.add_cylinder(0, -0.26, 0, 0.045, 0.045, 0.38, 10)
        p_s.add_box(0, -0.48, 0, 0.32, 0.04, 0.06)
        p_s.add_box(0, -0.48, 0, 0.06, 0.04, 0.32)
    elif mesh_id == "METAL_CABINET":
        p.add_chamfered_box(0, 0, 0, 0.54, 0.86, 0.35, 0.025)
        p_s.add_box(-0.26, 0, 0.36, 0.24, 0.78, 0.018)
        p_s.add_box(0.26, 0, 0.36, 0.24, 0.78, 0.018)
    elif mesh_id == "GEAR_WHEEL":
        p.add_cylinder(0, 0, 0, 0.38, 0.38, 0.18, 14)
        p_s.add_cylinder(0, 0, 0, 0.14, 0.14, 0.26, 10)
        for i in range(14):
            a = (i / 14.0) * TAU
            p.add_box(math.cos(a) * 0.43, 0, math.sin(a) * 0.43, 0.05, 0.08, 0.05)
    elif mesh_id == "AXLE_ROD":
        p.add_chamfered_box(0, 0, 0, 0.94, 0.09, 0.09, 0.02)
        p_s.add_box(-0.84, 0, 0, 0.08, 0.13, 0.13)
        p_s.add_box(0.84, 0, 0, 0.08, 0.13, 0.13)
    elif mesh_id == "SUSPENSION_BOGIE":
        p.add_chamfered_box(0, 0.08, 0, 0.72, 0.12, 0.34, 0.02)
        for sx in (-0.48, 0.48):
            for sz in (-0.40, 0.40):
                p_s.add_cylinder(sx, -0.14, sz, 0.18, 0.18, 0.14, 12, axis="z")
    elif mesh_id == "SOFT_FOAM_BLOCK":
        p.add_chamfered_box(0, 0, 0, 0.50, 0.42, 0.50, 0.09)
    elif mesh_id == "BUOYANCY_FLOAT":
        p.add_chamfered_box(0, 0, 0, 0.45, 0.45, 0.45, 0.05)
        p_s.add_torus(0, 0.26, 0, 0.34, 0.03, 14, 6)
    elif mesh_id == "CEILING_LIGHT":
        p_s.add_box(0, 0.86, 0, 0.92, 0.14, 0.85)
        p_s.add_chamfered_box(0, -0.15, 0, 1.0, 0.38, 1.0, 0.05)
        p.add_box(0, -0.78, 0, 0.92, 0.22, 0.86)
    elif mesh_id == "DOOR_FRAME":
        p.add_chamfered_box(-0.91, 0, 0, 0.09, 1.0, 1.0, 0.02)
        p.add_chamfered_box(0.91, 0, 0, 0.09, 1.0, 1.0, 0.02)
        p.add_chamfered_box(0, 0.92, 0, 1.0, 0.08, 1.0, 0.02)
        p_s.add_box(0, -0.97, 0, 0.82, 0.03, 0.92)
    elif mesh_id == "WINDOW_FRAME":
        p.add_chamfered_box(-0.93, 0, 0, 0.07, 1.0, 1.0, 0.02)
        p.add_chamfered_box(0.93, 0, 0, 0.07, 1.0, 1.0, 0.02)
        p.add_chamfered_box(0, 0.92, 0, 1.0, 0.08, 1.0, 0.02)
        p.add_chamfered_box(0, -0.92, 0, 1.0, 0.08, 1.0, 0.02)
        p_s.add_box(0, 0, 0, 0.045, 0.86, 0.72)
    elif mesh_id == "WALL_CABINET":
        p.add_chamfered_box(0, 0, -0.08, 1.0, 1.0, 0.92, 0.04)
        p_s.add_chamfered_box(-0.48, -0.02, 0.90, 0.45, 0.90, 0.07, 0.02)
        p_s.add_chamfered_box(0.48, -0.02, 0.90, 0.45, 0.90, 0.07, 0.02)
    elif mesh_id == "LAB_TABLE":
        p.add_chamfered_box(0, 0.92, 0, 1.0, 0.08, 1.0, 0.02)
        for sx in (-0.88, 0.88):
            for sz in (-0.86, 0.86):
                p_s.add_box(sx, -0.08, sz, 0.06, 0.92, 0.06)
        p_s.add_box(0, -0.62, -0.84, 0.86, 0.045, 0.045)
    elif mesh_id == "CNC_MACHINE":
        p.add_chamfered_box(0, -0.62, 0, 1.0, 0.38, 0.96, 0.04)
        p.add_chamfered_box(0, 0.30, -0.58, 0.96, 0.58, 0.38, 0.04)
        p.add_chamfered_box(0, 0.90, -0.10, 0.98, 0.10, 0.86, 0.03)
        p_s.add_cylinder(0, 0.48, 0.15, 0.18, 0.22, 0.62, 12)
        p_s.add_box(0, -0.12, 0.20, 0.62, 0.12, 0.42)
    elif mesh_id == "HYDRAULIC_PRESS":
        p.add_chamfered_box(0, -0.88, 0, 1.0, 0.12, 0.96, 0.03)
        p.add_chamfered_box(-0.82, 0, 0, 0.16, 1.0, 0.52, 0.03)
        p.add_chamfered_box(0.82, 0, 0, 0.16, 1.0, 0.52, 0.03)
        p.add_chamfered_box(0, 0.72, 0, 0.98, 0.18, 0.56, 0.03)
        p_s.add_cylinder(0, 0.65, 0, 0.24, 0.24, 0.70, 14)
    elif mesh_id == "ROOF_TRUSS":
        p.add_box(0, 0.90, 0, 1.0, 0.10, 0.95)
        p.add_box(0, -0.90, 0, 1.0, 0.10, 0.95)
        for i in range(-4, 5):
            p_s.add_box(i * 0.23, 0, 0, 0.035, 0.86, 0.70)
    elif mesh_id == "STAIR_FLIGHT":
        steps = 8
        sd = 2.0 / steps
        sr = 2.0 / steps
        for i in range(steps):
            yt = -1.0 + (i + 1) * sr
            zc = 1.0 - (i + 0.5) * sd
            sb = -1.0 + max(0, i - 1) * sr
            p.add_box(0, (yt + sb) * 0.5, zc, 0.88, max(0.06, (yt - sb) * 0.5), sd * 0.52)
        for sx in (-0.94, 0.94):
            p_s.add_box(sx, 0.0, 0.0, 0.055, 0.48, 0.12)
    elif mesh_id == "PIPE_CONDUIT":
        p.add_cylinder(0, 0, 0, 0.65, 0.65, 2.0, 12, axis="x")
        for fx in (-0.92, -0.32, 0.32, 0.92):
            p_s.add_box(fx, 0, 0, 0.06, 0.98, 0.98)
        p_s.add_torus(0, 0.92, 0, 0.32, 0.06, 12, 6)
    elif mesh_id == "SAFETY_RAIL":
        p_a = b.part("RailSafety", m_accent)
        for px in (-0.92, 0.0, 0.92):
            p_a.add_box(px, 0, 0, 0.06, 1.0, 0.45)
        p_a.add_box(0, 0.92, 0, 1.0, 0.08, 0.48)
        p_a.add_box(0, 0.08, 0, 1.0, 0.06, 0.38)
        p_a.add_box(0, -0.78, 0, 1.0, 0.12, 0.25)
    elif mesh_id == "WARNING_STRIPE_CURB":
        p.add_chamfered_box(0, -0.25, 0, 1.0, 0.75, 0.98, 0.18)
        for sx in (-0.65, 0.0, 0.65):
            p_s.add_box(sx, 0.56, 0, 0.14, 0.40, 0.55)
    else:  # CEILING_VENT
        p.add_box(0, 0.72, 0, 0.94, 0.28, 0.94)
        p.add_chamfered_box(0, 0.10, 0, 1.0, 0.45, 1.0, 0.06)
        for i in range(-3, 4):
            p_s.add_box(i * 0.26, -0.58, 0, 0.045, 0.24, 0.88)

    return b


def main():
    models_root = os.path.join("app", "src", "main", "assets", "models")
    manifest = {"version": 1, "assets": {}}

    def emit(key, rel_path, builder):
        out_path = os.path.join("app", "src", "main", "assets", rel_path)
        verts, tris = builder.export(out_path)
        manifest["assets"][key] = {
            "path": rel_path.replace("\\", "/"),
            "vertices": verts,
            "triangles": tris,
            "sockets": builder.sockets,
        }

    # 1. Six Firearms
    emit("FIREARM_GLOCK18", "models/firearms/glock18_style.glb", build_firearm_glock18())
    emit("FIREARM_SERVICE_PISTOL", "models/firearms/service_pistol.glb", build_firearm_service_pistol())
    emit("FIREARM_MP5", "models/firearms/mp5_style.glb", build_firearm_mp5())
    emit("FIREARM_M4A1", "models/firearms/m4a1_style.glb", build_firearm_m4a1())
    emit("FIREARM_SHOTGUN", "models/firearms/pump_shotgun.glb", build_firearm_shotgun())
    emit("FIREARM_SNIPER", "models/firearms/bolt_sniper.glb", build_firearm_sniper())

    # 2. Six Ammo Boxes
    ammo_specs = [
        ("AMMO_BOX_9MM_AUTO", "models/ammo/ammo_box_9mm_pistol.glb", 0.20, 0.13, 0.14, (0.24, 0.26, 0.28), (0.88, 0.52, 0.14), "pistol"),
        ("AMMO_BOX_45_SERVICE", "models/ammo/ammo_box_45_service.glb", 0.20, 0.13, 0.14, (0.22, 0.25, 0.30), (0.28, 0.50, 0.72), "pistol"),
        ("AMMO_BOX_SMG_9MM", "models/ammo/ammo_box_smg_9mm.glb", 0.24, 0.14, 0.16, (0.20, 0.24, 0.26), (0.22, 0.64, 0.62), "pistol"),
        ("AMMO_BOX_556_CARBINE", "models/ammo/ammo_box_556_rifle.glb", 0.28, 0.16, 0.18, (0.23, 0.27, 0.20), (0.36, 0.58, 0.24), "rifle"),
        ("AMMO_BOX_12G_BUCKSHOT", "models/ammo/ammo_box_12g_shotgun.glb", 0.25, 0.15, 0.16, (0.22, 0.20, 0.21), (0.78, 0.20, 0.18), "shotgun"),
        ("AMMO_BOX_762_MATCH", "models/ammo/ammo_box_762_sniper.glb", 0.28, 0.15, 0.17, (0.24, 0.24, 0.22), (0.80, 0.64, 0.22), "rifle"),
    ]
    for key, rel, hx, hy, hz, sc, bc, ck in ammo_specs:
        emit(key, rel, build_ammo_box(key, hx, hy, hz, sc, bc, ck))

    # 3. 34 Standard Tools
    tool_paths = {
        "HAMMER_CLAW": "models/tools/hammer_claw.glb",
        "MALLET_HEAD": "models/tools/mallet_head.glb",
        "WRENCH_OPEN": "models/tools/wrench_open.glb",
        "RATCHET_SOCKET": "models/tools/ratchet_socket.glb",
        "SCREWDRIVER_DRIVER": "models/tools/screwdriver_driver.glb",
        "PLIERS_JAWS": "models/tools/pliers_jaws.glb",
        "HAND_SAW_BLADE": "models/tools/hand_saw_blade.glb",
        "POWER_DRILL": "models/tools/power_drill.glb",
        "ANGLE_GRINDER": "models/tools/angle_grinder.glb",
        "CIRCULAR_SAW": "models/tools/circular_saw.glb",
        "HEAT_GUN_NOZZLE": "models/tools/heat_gun_nozzle.glb",
        "WELDING_UNIT": "models/tools/welding_unit.glb",
        "WELDING_TORCH": "models/tools/welding_torch.glb",
        "CLAMP_C_FRAME": "models/tools/clamp_c_frame.glb",
        "HYDRAULIC_JACK": "models/tools/hydraulic_jack.glb",
        "HOIST_WINCH": "models/tools/hoist_winch.glb",
        "COMPRESSOR_TANK": "models/tools/compressor_tank.glb",
        "FAN_SHROUD": "models/tools/fan_shroud.glb",
        "GENERATOR_FRAME": "models/tools/generator_frame.glb",
        "DIGITAL_METER": "models/tools/digital_meter.glb",
        "THERMOMETER_PROBE": "models/tools/thermometer_probe.glb",
        "SCALE_PLATFORM": "models/tools/scale_platform.glb",
        "CALIPER_SLIDE": "models/tools/caliper_slide.glb",
        "PRESSURE_DIAL": "models/tools/pressure_dial.glb",
        "OPTICAL_MICROSCOPE": "models/tools/optical_microscope.glb",
        "LAB_BEAKER": "models/tools/lab_beaker.glb",
        "EXTINGUISHER_CYL": "models/tools/extinguisher_cyl.glb",
        "FLASHLIGHT_TORCH": "models/tools/flashlight_torch.glb",
        "WORKLIGHT_TRIPOD": "models/tools/worklight_tripod.glb",
        "DETECTOR_WAND": "models/tools/detector_wand.glb",
        "MAGNET_HORSESHOE": "models/tools/magnet_horseshoe.glb",
        "SUCTION_CUP_HANDLE": "models/tools/suction_cup_handle.glb",
        "PULLEY_BLOCK": "models/tools/pulley_block.glb",
        "SPIRIT_LEVEL": "models/tools/spirit_level.glb",
    }
    for key, rel in tool_paths.items():
        emit(key, rel, build_tool_glb(key))

    # 4. 53 Props & Architectural Meshes
    prop_paths = {
        "REINFORCED_CRATE": "models/containers/reinforced_crate.glb",
        "WOOD_PALLET_CRATE": "models/containers/wood_pallet_crate.glb",
        "GLASS_CONTAINER": "models/containers/glass_container.glb",
        "BARREL_DRUM": "models/containers/barrel_drum.glb",
        "TREADED_TIRE": "models/mechanical/treaded_tire.glb",
        "GYRO_FLYWHEEL": "models/mechanical/gyro_flywheel.glb",
        "SPRING_PISTON": "models/mechanical/spring_piston.glb",
        "GEAR_WHEEL": "models/mechanical/gear_wheel.glb",
        "AXLE_ROD": "models/mechanical/axle_rod.glb",
        "SUSPENSION_BOGIE": "models/mechanical/suspension_bogie.glb",
        "STEEL_IBEAM": "models/construction/steel_ibeam.glb",
        "CONCRETE_MASONRY": "models/construction/concrete_masonry.glb",
        "RAMP_WEDGE": "models/construction/ramp_wedge.glb",
        "CENTRIFUGE_POD": "models/laboratory/centrifuge_pod.glb",
        "LAB_CONSOLE": "models/laboratory/lab_console.glb",
        "LAB_TABLE": "models/laboratory/lab_table.glb",
        "GEODESIC_SPHERE": "models/experimental/geodesic_sphere.glb",
        "DOMINO_MONOLITH": "models/experimental/domino_monolith.glb",
        "SOFT_FOAM_BLOCK": "models/experimental/soft_foam_block.glb",
        "BUOYANCY_FLOAT": "models/experimental/buoyancy_float.glb",
        "LAB_WORKBENCH": "models/furniture/lab_workbench.glb",
        "OFFICE_CHAIR": "models/furniture/office_chair.glb",
        "METAL_CABINET": "models/furniture/metal_cabinet.glb",
        "WALL_CABINET": "models/furniture/wall_cabinet.glb",
        "BATTERY_UNIT": "models/electrical/battery_unit.glb",
        "SWITCH_BOX": "models/electrical/switch_box.glb",
        "LIGHT_BULB_LAMP": "models/electrical/light_bulb_lamp.glb",
        "LED_INDICATOR": "models/electrical/led_indicator.glb",
        "DC_MOTOR": "models/electrical/dc_motor.glb",
        "KINETIC_GENERATOR": "models/electrical/kinetic_generator.glb",
        "FUSE_BLOCK": "models/electrical/fuse_block.glb",
        "CIRCUIT_CONNECTOR": "models/electrical/circuit_connector.glb",
        "ELECTRICAL_PANEL": "models/electrical/electrical_panel.glb",
        "SHELF_RACK": "models/environment/shelf_rack.glb",
        "CONVEYOR_UNIT": "models/environment/conveyor_unit.glb",
        "SHIPPING_CONTAINER": "models/environment/shipping_container.glb",
        "SCAFFOLD_FRAME": "models/environment/scaffold_frame.glb",
        "FLOOR_SLAB": "models/environment/floor_slab.glb",
        "WALL_PANEL": "models/environment/wall_panel.glb",
        "PILLAR_COLUMN": "models/environment/pillar_column.glb",
        "GLASS_PARTITION": "models/environment/glass_partition.glb",
        "CRANE_BOOM": "models/environment/crane_boom.glb",
        "CEILING_LIGHT": "models/environment/ceiling_light.glb",
        "DOOR_FRAME": "models/environment/door_frame.glb",
        "WINDOW_FRAME": "models/environment/window_frame.glb",
        "CNC_MACHINE": "models/environment/cnc_machine.glb",
        "HYDRAULIC_PRESS": "models/environment/hydraulic_press.glb",
        "ROOF_TRUSS": "models/environment/roof_truss.glb",
        "STAIR_FLIGHT": "models/environment/stair_flight.glb",
        "PIPE_CONDUIT": "models/environment/pipe_conduit.glb",
        "SAFETY_RAIL": "models/environment/safety_rail.glb",
        "WARNING_STRIPE_CURB": "models/environment/warning_stripe_curb.glb",
        "CEILING_VENT": "models/environment/ceiling_vent.glb",
    }
    for key, rel in prop_paths.items():
        emit(key, rel, build_prop_glb(key))

    os.makedirs(models_root, exist_ok=True)
    manifest_path = os.path.join(models_root, "manifest.json")
    with open(manifest_path, "w", encoding="utf-8") as mf:
        json.dump(manifest, mf, indent=2)

    print(f"Generated & validated {len(manifest['assets'])} GLB models -> {manifest_path}")


if __name__ == "__main__":
    main()
