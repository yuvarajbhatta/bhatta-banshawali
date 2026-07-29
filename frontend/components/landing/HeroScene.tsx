"use client";

import { useMemo } from "react";
import { Scene } from "@/components/three/Scene";
import { Constellation, type ConstellationEdge, type ConstellationNode } from "@/components/three/Constellation";
import { useSceneQuality } from "@/components/three/useSceneQuality";
import styles from "./HeroScene.module.css";

/**
 * Purely decorative -- an abstract, unlabeled branching structure
 * evoking a family tree/star-map, sitting behind the hero copy. Not
 * built from real data (that's the dashboard's FamilyConstellation);
 * this is atmosphere.
 */
export function HeroScene() {
  const quality = useSceneQuality();
  const { nodes, edges } = useMemo(() => buildHeroGraph(quality), [quality]);

  return (
    <div className={styles.canvasWrapper} aria-hidden="true">
      <Scene className={styles.canvas} camera={{ position: [0, 0, 11], fov: 50 }}>
        <Constellation nodes={nodes} edges={edges} interactive={false} />
      </Scene>
    </div>
  );
}

function buildHeroGraph(quality: "low" | "high"): { nodes: ConstellationNode[]; edges: ConstellationEdge[] } {
  const nodes: ConstellationNode[] = [
    { id: "root", label: "", position: [0, 2.4, -1], color: "#c9a227", emphasis: true },
  ];
  const edges: ConstellationEdge[] = [];

  const branchCount = quality === "low" ? 2 : 3;
  for (let i = 0; i < branchCount; i++) {
    const angle = (i / Math.max(branchCount - 1, 1) - 0.5) * Math.PI * 0.8;
    const bx = Math.sin(angle) * 3.4;
    const by = 0.2;
    const bz = Math.cos(angle) * -1.5;
    const branchId = `branch-${i}`;
    nodes.push({ id: branchId, label: "", position: [bx, by, bz], color: "#15803d" });
    edges.push({ from: "root", to: branchId });

    const leafCount = quality === "low" ? 1 : 1 + (i % 2);
    for (let j = 0; j < leafCount; j++) {
      const leafId = `leaf-${i}-${j}`;
      const lx = bx + (j - (leafCount - 1) / 2) * 1.5;
      const ly = by - 2.4;
      const lz = bz + 1;
      nodes.push({ id: leafId, label: "", position: [lx, ly, lz], color: "#14532d" });
      edges.push({ from: branchId, to: leafId });
    }
  }

  return { nodes, edges };
}
