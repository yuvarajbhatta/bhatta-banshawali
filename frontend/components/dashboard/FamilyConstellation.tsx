"use client";

import { useMemo } from "react";
import { useRouter } from "@/i18n/navigation";
import { Scene } from "@/components/three/Scene";
import { Constellation, type ConstellationEdge, type ConstellationNode } from "@/components/three/Constellation";
import { useSceneQuality } from "@/components/three/useSceneQuality";
import type { FamilySnapshotDto, PersonSummaryDto } from "@/lib/api";
import styles from "./FamilyConstellation.module.css";

interface FamilyConstellationProps {
  self: PersonSummaryDto;
  family: FamilySnapshotDto;
  fatherLabel: string;
  motherLabel: string;
}

export function FamilyConstellation({ self, family, fatherLabel, motherLabel }: FamilyConstellationProps) {
  const router = useRouter();
  const quality = useSceneQuality();

  const { nodes, edges } = useMemo(
    () => buildFamilyGraph(self, family, fatherLabel, motherLabel, quality),
    [self, family, fatherLabel, motherLabel, quality],
  );

  function handleSelect(personId: number) {
    if (personId !== self.id) {
      router.push(`/directory/${personId}`);
    }
  }

  // Small arrays (immediate family only) -- remapping on every render to
  // attach the click handler is cheap, no memoization needed here.
  const nodesWithHandlers = nodes.map((node) => ({ ...node, onSelect: () => handleSelect(Number(node.id)) }));

  return (
    <div className={styles.canvasWrapper}>
      <Scene
        className={styles.canvas}
        camera={{ position: [0, 0.5, 9], fov: 50 }}
        fallback={<span className={styles.fallbackLabel}>{self.englishFullName}</span>}
      >
        <Constellation nodes={nodesWithHandlers} edges={edges} />
      </Scene>
    </div>
  );
}

function buildFamilyGraph(
  self: PersonSummaryDto,
  family: FamilySnapshotDto,
  fatherLabel: string,
  motherLabel: string,
  quality: "low" | "high",
): { nodes: ConstellationNode[]; edges: ConstellationEdge[] } {
  const nodes: ConstellationNode[] = [
    { id: String(self.id), label: self.englishFullName, position: [0, 0, 0], color: "#c9a227", emphasis: true },
  ];
  const edges: ConstellationEdge[] = [];

  if (family.father) {
    nodes.push({
      id: String(family.father.id),
      label: family.father.englishFullName,
      sublabel: fatherLabel,
      position: [-2.4, 1.9, -1],
      color: "#14532d",
    });
    edges.push({ from: String(self.id), to: String(family.father.id) });
  }

  if (family.mother) {
    nodes.push({
      id: String(family.mother.id),
      label: family.mother.englishFullName,
      sublabel: motherLabel,
      position: [2.4, 1.9, -1],
      color: "#14532d",
    });
    edges.push({ from: String(self.id), to: String(family.mother.id) });
  }

  // On small screens, cap how many spouse/children nodes render at once
  // -- a dense constellation is hard to read (and heavier to compute)
  // on a phone-sized canvas.
  const maxSideNodes = quality === "low" ? 3 : family.spouses.length;
  const maxChildNodes = quality === "low" ? 4 : family.children.length;

  family.spouses.slice(0, maxSideNodes).forEach((spouse, index) => {
    const side = index % 2 === 0 ? -1 : 1;
    const distance = 2.6 + Math.floor(index / 2) * 1.6;
    nodes.push({
      id: String(spouse.id),
      label: spouse.englishFullName,
      position: [side * distance, 0, 1.4],
      color: "#15803d",
    });
    edges.push({ from: String(self.id), to: String(spouse.id) });
  });

  const children = family.children.slice(0, maxChildNodes);
  const childCount = children.length;
  children.forEach((child, index) => {
    const spread = Math.min(childCount - 1, 4);
    const t = childCount === 1 ? 0.5 : index / (childCount - 1);
    const x = spread === 0 ? 0 : (t - 0.5) * (spread * 1.8 + 1.5);
    nodes.push({
      id: String(child.id),
      label: child.englishFullName,
      position: [x, -2.1, 0.6],
      color: "#166534",
    });
    edges.push({ from: String(self.id), to: String(child.id) });
  });

  return { nodes, edges };
}
