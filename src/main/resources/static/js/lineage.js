let currentZoom = 1;
const MIN_ZOOM = 0.4;
const MAX_ZOOM = 2;
const ZOOM_STEP = 0.1;

let nodeIdCounter = 1;
let rootNode = null;
const messages = window.lineageMessages || {};

function t(key) {
    return messages[key] || '';
}

function formatMessage(template, ...values) {
    return (template || '').replace(/\{(\d+)}/g, (_, index) => values[Number(index)] ?? '');
}

function getCsrfHeaders() {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

    if (!csrfToken || !csrfHeader) {
        return {};
    }

    return {
        [csrfHeader]: csrfToken
    };
}

function createNode(name = t('defaultName') || 'Type Name Here', parentId = null, generationNumber = null) {
    return {
        id: nodeIdCounter++,
        dbId: null,
        parentDbId: parentId,
        generationNumber,
        name,
        children: []
    };
}

function mapServerNodeToUiNode(serverNode) {
    if (!serverNode || !serverNode.dbId) {
        return null;
    }

    const node = {
        id: nodeIdCounter++,
        dbId: serverNode.dbId,
        parentDbId: serverNode.parentDbId ?? null,
        generationNumber: serverNode.generationNumber ?? null,
        name: serverNode.name || t('defaultName') || 'Type Name Here',
        children: []
    };

    if (Array.isArray(serverNode.children)) {
        node.children = serverNode.children
            .map(child => mapServerNodeToUiNode(child))
            .filter(Boolean);
    }

    return node;
}

async function loadLineageTree() {
    try {
        const response = await fetch('/lineage/tree');
        if (!response.ok) {
            throw new Error(t('loadFailed') || 'Could not load lineage tree');
        }

        const data = await response.json();

        if (!data || !data.dbId) {
            rootNode = null;
            renderTree();
            return;
        }

        nodeIdCounter = 1;
        rootNode = mapServerNodeToUiNode(data);
        renderTree();
    } catch (error) {
        console.error(t('loadFailed') || 'Could not load lineage tree', error);
        rootNode = null;
        renderTree();
    }
}

function startRootNode() {
    if (rootNode !== null) {
        const replace = confirm(t('rootExists') || 'A lineage already exists on this page. Replace it and start over?');
        if (!replace) {
            return;
        }
    }

    const rootName = prompt(t('rootPrompt') || 'Enter the root ancestor name:', 'Jhanka Nath');
    if (rootName === null) {
        return;
    }

    rootNode = createNode(rootName.trim() || t('defaultName') || 'Type Name Here', null, 1);
    renderTree();
    askAndAddChildren(rootNode);
}

function resetBoard() {
    const confirmed = confirm('Clear the entire lineage board?');
    if (!confirmed) {
        return;
    }

    rootNode = null;
    renderTree();
}
function askAndAddChildren(node) {
    const existingCount = Array.isArray(node.children) ? node.children.length : 0;
    const input = prompt(
        formatMessage(
            t('childrenPrompt') || 'How many NEW sons do you want to add under {0}?\nExisting sons already on this branch: {1}',
            node.name || t('personFallback') || 'this person',
            existingCount
        ),
        '1'
    );

    if (input === null) {
        return;
    }

    const count = Number(input);
    if (!Number.isInteger(count) || count < 0) {
        alert(t('wholeNumber') || 'Please enter a whole number like 0, 1, 2, 3, ...');
        return;
    }

    if (!Array.isArray(node.children)) {
        node.children = [];
    }

    const nextGeneration = node.generationNumber != null ? node.generationNumber + 1 : null;
    for (let i = 0; i < count; i++) {
        node.children.push(createNode(t('defaultName') || 'Type Name Here', node.dbId, nextGeneration));
    }

    renderTree();
}

function findNodeById(node, id) {
    if (!node) {
        return null;
    }

    if (node.id === id) {
        return node;
    }

    for (const child of node.children) {
        const found = findNodeById(child, id);
        if (found) {
            return found;
        }
    }

    return null;
}

function findParentNode(currentNode, targetId) {
    if (!currentNode || !currentNode.children) {
        return null;
    }

    for (const child of currentNode.children) {
        if (child.id === targetId) {
            return currentNode;
        }

        const found = findParentNode(child, targetId);
        if (found) {
            return found;
        }
    }

    return null;
}

function removeNodeById(parent, id) {
    if (!parent) {
        return false;
    }

    const index = parent.children.findIndex(child => child.id === id);
    if (index !== -1) {
        parent.children.splice(index, 1);
        return true;
    }

    for (const child of parent.children) {
        if (removeNodeById(child, id)) {
            return true;
        }
    }

    return false;
}

function updateNodeName(nodeId, element) {
    const node = findNodeById(rootNode, nodeId);
    if (!node) {
        return;
    }

    node.name = element.textContent.trim() || t('defaultName') || 'Type Name Here';
}

function handleAddSons(nodeId) {
    const node = findNodeById(rootNode, nodeId);
    if (!node) {
        return;
    }

    const trimmedName = (node.name || '').trim();
    if (!trimmedName || trimmedName === (t('defaultName') || 'Type Name Here')) {
        alert(t('enterNameBeforeAdd') || "Please type the person's name in this box before adding sons.");
        return;
    }

    askAndAddChildren(node);
}

async function handleSaveNode(nodeId) {
    const node = findNodeById(rootNode, nodeId);
    if (!node) {
        return;
    }

    const trimmedName = (node.name || '').trim();
    if (!trimmedName || trimmedName === (t('defaultName') || 'Type Name Here')) {
        alert(t('enterRealName') || 'Please type a real name before saving.');
        return;
    }

    const parentNode = rootNode && rootNode.id !== node.id ? findParentNode(rootNode, node.id) : null;
    if (parentNode && !parentNode.dbId) {
        await handleSaveNode(parentNode.id);
    }
    if (parentNode && parentNode.dbId) {
        node.parentDbId = parentNode.dbId;
    }

    try {
        const params = new URLSearchParams();
        params.append('fullName', trimmedName);

        if (node.dbId != null) {
            params.append('personId', node.dbId);
        }

        if (node.parentDbId != null && node.dbId == null) {
            params.append('parentId', node.parentDbId);
        }

        if (node.generationNumber != null) {
            params.append('generationNumber', node.generationNumber);
        }

        const response = await fetch('/lineage/save-person', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                ...getCsrfHeaders()
            },
            body: params.toString()
        });

        if (!response.ok) {
            const errorText = await response.text();
            console.error('Save failed:', errorText);
            throw new Error(t('saveFailed') || 'Save failed');
        }

        const data = await response.json();
        node.dbId = data.id;

        for (const child of node.children) {
            child.parentDbId = node.dbId;
            if (child.generationNumber == null && node.generationNumber != null) {
                child.generationNumber = node.generationNumber + 1;
            }
        }

        renderTree();
        alert(t('savedSuccessfully') || 'Saved successfully.');
    } catch (error) {
        console.error(error);
        alert(t('nodeSaveFailed') || 'Could not save this node.');
    }
}

async function saveTreeRecursively(node) {
    if (!node) {
        return;
    }

    const trimmedName = (node.name || '').trim();
    if (!trimmedName || trimmedName === (t('defaultName') || 'Type Name Here')) {
        throw new Error(t('fillAllNames') || 'Please fill in every node name before saving.');
    }

    const params = new URLSearchParams();
    params.append('fullName', trimmedName);

    if (node.dbId != null) {
        params.append('personId', node.dbId);
    }

    if (node.parentDbId != null && node.dbId == null) {
        params.append('parentId', node.parentDbId);
    }

    if (node.generationNumber != null) {
        params.append('generationNumber', node.generationNumber);
    }

    const response = await fetch('/lineage/save-person', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            ...getCsrfHeaders()
        },
        body: params.toString()
    });

    if (!response.ok) {
        const errorText = await response.text();
        console.error('Save failed:', errorText);
        throw new Error(t('treeSaveFailed') || 'Could not save lineage tree.');
    }

    const data = await response.json();
    node.dbId = data.id;

    for (const child of node.children) {
        child.parentDbId = node.dbId;
        if (child.generationNumber == null && node.generationNumber != null) {
            child.generationNumber = node.generationNumber + 1;
        }
        await saveTreeRecursively(child);
    }
}

async function handleSaveAll() {
    if (!rootNode) {
        alert(t('noTree') || 'There is no lineage tree to save.');
        return;
    }

    try {
        await saveTreeRecursively(rootNode);
        renderTree();
        alert(t('treeSaved') || 'Entire lineage saved successfully.');
    } catch (error) {
        console.error(error);
        alert(error.message || t('treeSaveFailed') || 'Could not save the lineage tree.');
    }
}

function applyZoom() {
    const wrapper = document.querySelector('.tree-wrapper');
    const label = document.getElementById('zoomLabel');

    if (wrapper) {
        wrapper.style.transform = `scale(${currentZoom})`;
        wrapper.style.transformOrigin = 'top left';
    }

    if (label) {
        label.textContent = `${Math.round(currentZoom * 100)}%`;
    }
}

function zoomIn() {
    currentZoom = Math.min(MAX_ZOOM, currentZoom + ZOOM_STEP);
    applyZoom();
}

function zoomOut() {
    currentZoom = Math.max(MIN_ZOOM, currentZoom - ZOOM_STEP);
    applyZoom();
}

function resetZoom() {
    currentZoom = 1;
    applyZoom();
}

function handleDeleteNode(nodeId) {
    if (!rootNode) {
        return;
    }

    if (rootNode.id === nodeId) {
        const confirmed = confirm(t('rootDelete') || 'Delete the entire lineage root?');
        if (confirmed) {
            rootNode = null;
            renderTree();
        }
        return;
    }

    const confirmed = confirm(t('branchDelete') || 'Delete this branch?');
    if (!confirmed) {
        return;
    }

    removeNodeById(rootNode, nodeId);
    renderTree();
}

function buildTreeHtml(node) {
    const childrenHtml = node.children.length > 0
        ? '<ul>' + node.children.map(child => buildTreeHtml(child)).join('') + '</ul>'
        : '';

    return `
        <li>
            <div class="node-box">
                <div class="node-name" contenteditable="true" onblur="updateNodeName(${node.id}, this)">${escapeHtml(node.name)}</div>
                <div class="node-meta">${node.dbId ? (t('savedId') || 'Saved ID:') + ' ' + node.dbId : (t('notSaved') || 'Not saved yet')}</div>
                <div class="node-actions">
                    <button type="button" onclick="handleAddSons(${node.id})">${t('addSons') || 'Add Sons'}</button>
                    <button type="button" onclick="handleDeleteNode(${node.id})">${t('delete') || 'Delete'}</button>
                </div>
            </div>
            ${childrenHtml}
        </li>
    `;
}

function renderTree() {
    const treeContainer = document.getElementById('treeContainer');
    const emptyState = document.getElementById('emptyState');

    if (!treeContainer || !emptyState) {
        return;
    }

    if (!rootNode) {
        treeContainer.innerHTML = '';
        emptyState.style.display = 'block';
        return;
    }

    emptyState.style.display = 'none';
    treeContainer.innerHTML = `<div class="tree-wrapper"><ul class="tree">${buildTreeHtml(rootNode)}</ul></div>`;
    applyZoom();
}

function scrollToTopSmooth() {
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function toggleBackToTopButton() {
    const button = document.getElementById('backToTopButton');
    if (!button) {
        return;
    }

    if (window.scrollY > 250) {
        button.style.display = 'block';
    } else {
        button.style.display = 'none';
    }
}

function escapeHtml(text) {
    return text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

window.addEventListener('scroll', toggleBackToTopButton);
window.addEventListener('DOMContentLoaded', loadLineageTree);
window.handleSaveAll = handleSaveAll;
window.startRootNode = startRootNode;
window.resetBoard = resetBoard;
window.scrollToTopSmooth = scrollToTopSmooth;
window.handleAddSons = handleAddSons;
window.handleDeleteNode = handleDeleteNode;
window.updateNodeName = updateNodeName;
window.zoomIn = zoomIn;
window.zoomOut = zoomOut;
window.resetZoom = resetZoom;
