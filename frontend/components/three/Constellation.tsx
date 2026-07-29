"use client";

import { useMemo, useRef, useState } from "react";
import { useFrame } from "@react-three/fiber";
import { Html, Line, OrbitControls } from "@react-three/drei";
import type * as THREE from "three";
import styles from "./Constellation.module.css";

export interface ConstellationNode {
  id: string;
  label: string;
  sublabel?: string;
  position: [number, number, number];
  color?: string;
  /** Slightly larger + brighter -- used for the "self" node in a family view. */
  emphasis?: boolean;
  onSelect?: () => void;
}

export interface ConstellationEdge {
  from: string;
  to: string;
}

interface ConstellationProps {
  nodes: ConstellationNode[];
  edges: ConstellationEdge[];
  autoRotate?: boolean;
  interactive?: boolean;
}

/**
 * A rotating, orbit-able cluster of glowing connected nodes with crisp
 * HTML name labels (drei's Html, locked to each node's 3D position) --
 * the shared visual behind both the landing hero (abstract/decorative
 * nodes) and the dashboard family preview (real people, clickable).
 */
export function Constellation({ nodes, edges, autoRotate = true, interactive = true }: ConstellationProps) {
  const groupRef = useRef<THREE.Group>(null);
  const [paused, setPaused] = useState(false);

  const nodeById = useMemo(() => new Map(nodes.map((node) => [node.id, node])), [nodes]);

  useFrame((_, delta) => {
    if (autoRotate && !paused && groupRef.current) {
      groupRef.current.rotation.y += delta * 0.12;
    }
  });

  return (
    <>
      <ambientLight intensity={0.6} />
      <pointLight position={[10, 10, 10]} intensity={1.2} color="#e8c688" />
      <pointLight position={[-10, -6, -10]} intensity={0.5} color="#b85c3e" />

      <group ref={groupRef} onPointerOver={() => setPaused(true)} onPointerOut={() => setPaused(false)}>
        {edges.map((edge) => {
          const from = nodeById.get(edge.from);
          const to = nodeById.get(edge.to);
          if (!from || !to) {
            return null;
          }
          return (
            <Line
              key={`${edge.from}-${edge.to}`}
              points={[from.position, to.position]}
              color="#cf9f4d"
              transparent
              opacity={0.35}
              lineWidth={1}
            />
          );
        })}

        {nodes.map((node) => (
          <ConstellationNodeMesh key={node.id} node={node} />
        ))}
      </group>

      {interactive ? (
        <OrbitControls
          enablePan={false}
          minDistance={4}
          maxDistance={16}
          minPolarAngle={Math.PI / 3}
          maxPolarAngle={(Math.PI * 2) / 3}
        />
      ) : null}
    </>
  );
}

function ConstellationNodeMesh({ node }: { node: ConstellationNode }) {
  const [hovered, setHovered] = useState(false);
  const baseScale = node.emphasis ? 1.4 : 1;
  const color = node.color ?? "#b85c3e";

  return (
    <group position={node.position}>
      <mesh
        scale={hovered ? baseScale * 1.2 : baseScale}
        onPointerOver={(event) => {
          event.stopPropagation();
          setHovered(true);
        }}
        onPointerOut={() => setHovered(false)}
        onClick={node.onSelect}
      >
        <sphereGeometry args={[0.35, 32, 32]} />
        <meshStandardMaterial
          color={color}
          emissive={color}
          emissiveIntensity={hovered ? 1.2 : 0.6}
          roughness={0.3}
          metalness={0.4}
        />
      </mesh>
      {node.label ? (
        <Html center distanceFactor={8} style={{ pointerEvents: "none" }}>
          <div className={styles.label}>
            <span>{node.label}</span>
            {node.sublabel ? <span className={styles.sublabel}>{node.sublabel}</span> : null}
          </div>
        </Html>
      ) : null}
    </group>
  );
}
