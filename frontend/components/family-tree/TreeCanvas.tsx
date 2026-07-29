"use client";

import { useEffect, useMemo, useRef } from "react";
import {
  Background,
  BackgroundVariant,
  MiniMap,
  ReactFlow,
  useReactFlow,
  type NodeTypes,
} from "@xyflow/react";
import "@xyflow/react/dist/style.css";
import type { PersonTreeNodeDto } from "@/lib/api";
import { MemberNode } from "./MemberNode";
import { TreeControls } from "./TreeControls";
import { TreeLegend } from "./TreeLegend";
import { useFamilyTreeLayout } from "./useFamilyTreeLayout";
import styles from "./TreeCanvas.module.css";

const NODE_TYPES: NodeTypes = { member: MemberNode };

interface TreeCanvasProps {
  people: PersonTreeNodeDto[];
  selectedId: number | null;
  focusId: number | null;
  onSelect: (personId: number) => void;
}

export function TreeCanvas({ people, selectedId, focusId, onSelect }: TreeCanvasProps) {
  const { nodes: layoutNodes, edges } = useFamilyTreeLayout(people, selectedId);
  const { setCenter, fitView } = useReactFlow();
  const lastFocusedIdRef = useRef<number | null>(null);

  const nodes = useMemo(
    () => layoutNodes.map((node) => ({ ...node, data: { ...node.data, onSelect } })),
    [layoutNodes, onSelect],
  );

  useEffect(() => {
    if (!focusId || lastFocusedIdRef.current === focusId) {
      return;
    }
    const target = nodes.find((node) => node.id === String(focusId));
    if (target) {
      lastFocusedIdRef.current = focusId;
      const x = target.position.x + (target.width ?? 216) / 2;
      const y = target.position.y + (target.height ?? 88) / 2;
      window.requestAnimationFrame(() => setCenter(x, y, { zoom: 1, duration: 400 }));
    }
  }, [focusId, nodes, setCenter]);

  useEffect(() => {
    if (focusId) {
      return;
    }
    window.requestAnimationFrame(() => fitView({ padding: 0.2, duration: 300 }));
    // Re-fits whenever the filtered person count changes, not on every
    // reference change of `people` -- `people.length` in the deps array
    // (rather than `people` itself) is intentional here.
  }, [people.length, focusId, fitView]);

  return (
    <div className={styles.canvasWrapper}>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        nodeTypes={NODE_TYPES}
        nodesDraggable={false}
        nodesConnectable={false}
        elementsSelectable={false}
        panOnScroll
        zoomOnScroll
        minZoom={0.15}
        maxZoom={1.5}
        proOptions={{ hideAttribution: false }}
      >
        <Background variant={BackgroundVariant.Dots} gap={24} size={1} color="var(--color-border)" />
        <MiniMap pannable zoomable nodeColor="var(--color-primary-300)" maskColor="rgba(240, 253, 244, 0.6)" />
      </ReactFlow>
      <TreeControls />
      <TreeLegend />
    </div>
  );
}
