"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { ChevronDown, ChevronRight, Plus, Trash2 } from "lucide-react";
import { Button } from "@/components/Button";
import { saveLineagePerson, type LineageTreeNode } from "@/lib/api";
import styles from "./LineageBuilder.module.css";

interface EditableNode {
  key: number;
  dbId: number | null;
  parentDbId: number | null;
  generationNumber: number | null;
  name: string;
  children: EditableNode[];
}

let keyCounter = 1;

function fromServerNode(node: LineageTreeNode): EditableNode {
  return {
    key: keyCounter++,
    dbId: node.dbId,
    parentDbId: node.parentDbId,
    generationNumber: node.generationNumber,
    name: node.englishName || node.name || "",
    children: (node.children ?? []).map(fromServerNode),
  };
}

function findNode(node: EditableNode | null, key: number): EditableNode | null {
  if (!node) return null;
  if (node.key === key) return node;
  for (const child of node.children) {
    const found = findNode(child, key);
    if (found) return found;
  }
  return null;
}

function removeNode(parent: EditableNode, key: number): boolean {
  const index = parent.children.findIndex((child) => child.key === key);
  if (index !== -1) {
    parent.children.splice(index, 1);
    return true;
  }
  return parent.children.some((child) => removeNode(child, key));
}

async function persistNode(node: EditableNode, parentDbId: number | null): Promise<void> {
  const trimmed = node.name.trim();
  if (!trimmed) {
    throw new Error("empty-name");
  }
  const result = await saveLineagePerson({
    fullName: trimmed,
    personId: node.dbId ?? undefined,
    parentId: node.dbId == null && parentDbId != null ? parentDbId : undefined,
    generationNumber: node.generationNumber ?? undefined,
  });
  node.dbId = result.id;
  node.name = result.englishName || node.name;

  for (const child of node.children) {
    if (child.generationNumber == null && node.generationNumber != null) {
      child.generationNumber = node.generationNumber + 1;
    }
    await persistNode(child, node.dbId);
  }
}

interface LineageBuilderProps {
  initialTree: LineageTreeNode | null;
}

export function LineageBuilder({ initialTree }: LineageBuilderProps) {
  const t = useTranslations("adminLineagePage");
  const [root, setRoot] = useState<EditableNode | null>(() => (initialTree ? fromServerNode(initialTree) : null));
  const [collapsed, setCollapsed] = useState<Set<number>>(new Set());
  const [rootNameDraft, setRootNameDraft] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Node contents are mutated in place (the tree can be deep, and every
  // handler already needs a find-by-key walk), then the root is
  // shallow-cloned so React sees a new reference and re-renders --
  // avoids threading immutable-update logic through every recursive
  // helper for what's an internal editing tool, not public-facing state.
  function mutate(fn: (currentRoot: EditableNode) => void) {
    setRoot((current) => {
      if (!current) return current;
      fn(current);
      return { ...current };
    });
  }

  function startRoot() {
    const name = rootNameDraft.trim();
    if (!name) return;
    setRoot({ key: keyCounter++, dbId: null, parentDbId: null, generationNumber: 1, name, children: [] });
    setRootNameDraft("");
  }

  function addChild(parentKey: number) {
    mutate((currentRoot) => {
      const parent = findNode(currentRoot, parentKey);
      if (!parent) return;
      const nextGeneration = parent.generationNumber != null ? parent.generationNumber + 1 : null;
      parent.children.push({
        key: keyCounter++,
        dbId: null,
        parentDbId: parent.dbId,
        generationNumber: nextGeneration,
        name: "",
        children: [],
      });
    });
  }

  function rename(key: number, name: string) {
    mutate((currentRoot) => {
      const node = findNode(currentRoot, key);
      if (node) node.name = name;
    });
  }

  function remove(key: number) {
    if (!root) return;
    if (root.key === key) {
      if (!window.confirm(t("confirmDeleteRoot"))) return;
      setRoot(null);
      return;
    }
    if (!window.confirm(t("confirmDeleteBranch"))) return;
    mutate((currentRoot) => {
      removeNode(currentRoot, key);
    });
  }

  function toggleCollapse(key: number) {
    setCollapsed((current) => {
      const next = new Set(current);
      if (next.has(key)) {
        next.delete(key);
      } else {
        next.add(key);
      }
      return next;
    });
  }

  async function handleSaveAll() {
    if (!root) {
      setError(t("noTree"));
      return;
    }
    setSaving(true);
    setError(null);
    try {
      await persistNode(root, null);
      setRoot({ ...root });
    } catch {
      setError(t("saveError"));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div>
      <div className={styles.toolbar}>
        {!root ? (
          <>
            <input
              className={styles.nameInput}
              placeholder={t("rootNamePlaceholder")}
              value={rootNameDraft}
              onChange={(event) => setRootNameDraft(event.target.value)}
            />
            <Button variant="primary" onClick={startRoot} disabled={!rootNameDraft.trim()}>
              {t("startRoot")}
            </Button>
          </>
        ) : (
          <Button variant="primary" onClick={handleSaveAll} disabled={saving}>
            {saving ? t("saving") : t("saveAll")}
          </Button>
        )}
        {error ? <span className={styles.error}>{error}</span> : null}
        <span className={styles.hint}>{t("hint")}</span>
      </div>

      <div className={styles.board}>
        {!root ? (
          <p className={styles.hint}>{t("empty")}</p>
        ) : (
          <ul className={styles.tree}>
            <LineageNodeView
              node={root}
              collapsed={collapsed}
              onToggleCollapse={toggleCollapse}
              onRename={rename}
              onAddChild={addChild}
              onRemove={remove}
              t={t}
            />
          </ul>
        )}
      </div>
    </div>
  );
}

function LineageNodeView({
  node,
  collapsed,
  onToggleCollapse,
  onRename,
  onAddChild,
  onRemove,
  t,
}: {
  node: EditableNode;
  collapsed: Set<number>;
  onToggleCollapse: (key: number) => void;
  onRename: (key: number, name: string) => void;
  onAddChild: (key: number) => void;
  onRemove: (key: number) => void;
  t: ReturnType<typeof useTranslations>;
}) {
  const isCollapsed = collapsed.has(node.key);
  const hasChildren = node.children.length > 0;

  return (
    <li className={styles.nodeItem}>
      <div className={styles.nodeBox}>
        {hasChildren ? (
          <button type="button" className={styles.collapseButton} onClick={() => onToggleCollapse(node.key)} aria-label={t("toggle")}>
            {isCollapsed ? <ChevronRight size={14} /> : <ChevronDown size={14} />}
          </button>
        ) : null}
        <input
          className={styles.nameInput}
          value={node.name}
          placeholder={t("namePlaceholder")}
          onChange={(event) => onRename(node.key, event.target.value)}
          aria-label={t("nodeName")}
        />
        <span className={node.dbId != null ? `${styles.statusBadge} ${styles.savedBadge}` : styles.statusBadge}>
          {node.dbId != null ? t("saved", { id: node.dbId }) : t("notSaved")}
        </span>
        {hasChildren ? <span className={styles.childCount}>{t("childCount", { count: node.children.length })}</span> : null}
        <div className={styles.nodeActions}>
          <button type="button" className={styles.iconButton} onClick={() => onAddChild(node.key)} aria-label={t("addChild")}>
            <Plus size={14} />
          </button>
          <button type="button" className={styles.iconButton} onClick={() => onRemove(node.key)} aria-label={t("delete")}>
            <Trash2 size={14} />
          </button>
        </div>
      </div>

      {hasChildren && !isCollapsed ? (
        <ul className={styles.tree}>
          {node.children.map((child) => (
            <LineageNodeView
              key={child.key}
              node={child}
              collapsed={collapsed}
              onToggleCollapse={onToggleCollapse}
              onRename={onRename}
              onAddChild={onAddChild}
              onRemove={onRemove}
              t={t}
            />
          ))}
        </ul>
      ) : null}
    </li>
  );
}
