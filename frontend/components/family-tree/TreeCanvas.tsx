"use client";

import { useEffect, useMemo, useRef } from "react";
import {
  Background,
  BackgroundVariant,
  ReactFlow,
  useReactFlow,
  type NodeTypes,
} from "@xyflow/react";
import "@xyflow/react/dist/style.css";
import type { PersonTreeNodeDto } from "@/lib/api";
import { MemberNode } from "./MemberNode";
import { TreeControls } from "./TreeControls";
import { TouchHint } from "./TouchHint";
import { NODE_HEIGHT, NODE_WIDTH, useFamilyTreeLayout } from "./useFamilyTreeLayout";
import type { TreeHighlights } from "./treeHighlight";
import styles from "./TreeCanvas.module.css";

const NODE_TYPES: NodeTypes = { member: MemberNode };

// How far past the outermost nodes panning is still allowed -- enough
// room to comfortably center an edge node, not so much that panning
// drifts off into empty canvas with nothing in view.
const PAN_PADDING = 400;

interface TreeCanvasProps {
  people: PersonTreeNodeDto[];
  selectedId: number | null;
  focusId: number | null;
  onSelect: (personId: number) => void;
  highlights?: TreeHighlights;
}

export function TreeCanvas({ people, selectedId, focusId, onSelect, highlights }: TreeCanvasProps) {
  const { nodes: layoutNodes, edges } = useFamilyTreeLayout(people, selectedId, highlights);
  const { setCenter, fitView } = useReactFlow();
  const lastFocusedIdRef = useRef<number | null>(null);

  const nodes = useMemo(
    () => layoutNodes.map((node) => ({ ...node, data: { ...node.data, onSelect } })),
    [layoutNodes, onSelect],
  );

  const translateExtent = useMemo<[[number, number], [number, number]]>(() => {
    if (nodes.length === 0) {
      return [
        [-PAN_PADDING, -PAN_PADDING],
        [PAN_PADDING, PAN_PADDING],
      ];
    }
    const xs = nodes.map((node) => node.position.x);
    const ys = nodes.map((node) => node.position.y);
    return [
      [Math.min(...xs) - PAN_PADDING, Math.min(...ys) - PAN_PADDING],
      [Math.max(...xs) + NODE_WIDTH + PAN_PADDING, Math.max(...ys) + NODE_HEIGHT + PAN_PADDING],
    ];
  }, [nodes]);

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
        onlyRenderVisibleElements
        panOnScroll
        zoomOnScroll
        minZoom={0.15}
        maxZoom={1.5}
        translateExtent={translateExtent}
        proOptions={{ hideAttribution: false }}
      >
        <Background variant={BackgroundVariant.Dots} gap={24} size={1} color="var(--color-border)" />
      </ReactFlow>
      <TreeControls />
      <TouchHint />
    </div>
  );
}
