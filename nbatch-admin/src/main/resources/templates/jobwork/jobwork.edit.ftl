<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <#import "../common/common.macro.ftl" as netCommon>
    <@netCommon.commonStyle />
    <style>
        html, body { height: 100%; margin: 0; overflow: hidden; background: #edf3f1; }
        .flow-page { height: 100vh; display: flex; flex-direction: column; }
        .flow-toolbar { min-height: 52px; display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 6px 14px; background: #f8fbfa; border-bottom: 1px solid #d5e2de; }
        .flow-work-meta { min-width: 0; display: flex; align-items: center; flex-wrap: wrap; gap: 5px 12px; }
        .flow-title { font-size: 15px; font-weight: 600; color: #1e3739; white-space: nowrap; }
        .flow-work-meta-item { display: inline-flex; align-items: baseline; gap: 5px; min-width: 0; color: #64748b; font-size: 10px; }
        .flow-work-meta-item strong { max-width: 190px; overflow: hidden; color: #1e3739; font-size: 12px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
        .flow-work-meta-item.status strong { transition: color .16s; }
        .flow-work-meta-item.status strong.status-wait { color: #94a3b8; }
        .flow-work-meta-item.status strong.status-dispatched { color: #0891b2; }
        .flow-work-meta-item.status strong.status-running { color: #2563eb; }
        .flow-work-meta-item.status strong.status-complete { color: #16a34a; }
        .flow-work-meta-item.status strong.status-exception { color: #dc2626; }
        .flow-work-meta-item.status strong.status-skipped { color: #7c3aed; }
        .flow-work-meta-item.status strong.status-stopping { color: #d97706; }
        .flow-work-meta-item.status strong.status-stopped { color: #64748b; }
        .flow-work-meta-item.status strong.status-uninitialized { color: #475569; }
        .flow-actions { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; justify-content: flex-end; }
        .flow-action-cluster { display: flex; align-items: center; flex-wrap: wrap; gap: 5px; }
        .flow-actions .btn { height: 28px; padding: 0 9px; border-color: #b9d0cb; border-radius: 4px; background: #f9fcfb; color: #315b58; font-size: 12px; line-height: 26px; white-space: nowrap; box-shadow: none; transition: border-color .16s, background-color .16s, color .16s; }
        .flow-actions .btn:hover, .flow-actions .btn:focus { border-color: #5f9b93; background: #eaf6f3; color: #175d59; outline: 0; }
        .flow-actions .flow-action-primary { border-color: #2f7d73; background: #2f7d73; color: #fff; }
        .flow-actions .flow-action-primary:hover, .flow-actions .flow-action-primary:focus { border-color: #216f6b; background: #216f6b; color: #fff; }
        .flow-actions .flow-action-icon { width: 28px; padding: 0; text-align: center; }
        .flow-actions .dropdown-menu { min-width: 146px; margin-top: 5px; padding: 4px 0; border-color: #cbdcd8; border-radius: 4px; box-shadow: 0 5px 16px rgba(27, 67, 63, .14); }
        .flow-actions .dropdown-menu > li > a { padding: 7px 12px; color: #36504e; font-size: 12px; }
        .flow-actions .dropdown-menu > li > a:hover { background: #eef8f5; color: #175d59; }
        .flow-main { min-height: 0; flex: 1; display: grid; grid-template-columns: minmax(0, 1fr); gap: 10px; padding: 10px; }
        .flow-panel { min-width: 0; min-height: 0; display: flex; flex-direction: column; background: #fff; border: 1px solid #d5e2de; }
        .flow-panel-title { display: flex; align-items: center; justify-content: space-between; gap: 8px; padding: 7px 10px; border-bottom: 1px solid #e4edeb; color: #36504e; font-size: 13px; font-weight: 600; }
        .flow-panel-body { min-height: 0; flex: 1; overflow: auto; padding: 10px; }
        .flow-empty { padding: 26px 8px; color: #8a96a3; text-align: center; }
        .flow-canvas-wrap { position: relative; min-height: 0; flex: 1; overflow: auto; background: #f3f8f6; }
        .flow-canvas-wrap.is-connecting { cursor: crosshair; }
        .flow-canvas-selection-actions { position: absolute; z-index: 7; top: 0; left: 0; display: flex; align-items: center; gap: 2px; max-width: min(560px, calc(100% - 24px)); min-height: 32px; padding: 2px; border: 1px solid #d5e3df; border-radius: 3px; background: rgba(255, 255, 255, .88); box-shadow: 0 2px 8px rgba(43, 73, 68, .14); color: #52716c; }
        .flow-node-tooltip { position: absolute; z-index: 8; display: none; width: min(320px, calc(100% - 24px)); padding: 10px 12px; border: 1px solid #b8cfca; border-radius: 4px; background: rgba(255, 255, 255, .98); box-shadow: 0 8px 20px rgba(27, 67, 63, .18); color: #334155; font-size: 12px; line-height: 18px; pointer-events: none; }
        .flow-node-tooltip-title { overflow: hidden; color: #1e3739; font-size: 13px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
        .flow-node-tooltip-meta { display: grid; grid-template-columns: 68px minmax(0, 1fr); gap: 3px 8px; margin-top: 7px; }
        .flow-node-tooltip-meta dt { margin: 0; color: #718096; font-weight: normal; }
        .flow-node-tooltip-meta dd { min-width: 0; margin: 0; overflow-wrap: anywhere; color: #36504e; }
        .flow-canvas-selection-actions:after { position: absolute; bottom: -6px; left: 50%; width: 10px; height: 10px; border-right: 1px solid #d5e3df; border-bottom: 1px solid #d5e3df; background: rgba(255, 255, 255, .94); content: ''; transform: translateX(-50%) rotate(45deg); }
        .flow-canvas-selection-actions.is-below:after { top: -6px; bottom: auto; border-top: 1px solid #d5e3df; border-right: 0; border-bottom: 0; transform: translateX(-50%) rotate(45deg); }
        .flow-canvas-selection-actions[hidden] { display: none; }
        .flow-canvas-selection-actions .btn { width: 28px; height: 28px; padding: 0; border: 0; border-radius: 3px; background: transparent; color: #52716c; }
        .flow-canvas-selection-actions .btn:hover { background: #eef8f5; color: #216f6b; }
        .flow-canvas-selection-actions .flow-canvas-delete { color: #b45309; }
        .flow-canvas-selection-actions .flow-canvas-delete:hover { background: #fff7ed; color: #c2410c; }
        .flow-canvas-selection-actions.is-inline-selection { min-height: 18px; max-width: none; gap: 2px; padding: 1px; border: 1px solid rgba(184, 207, 202, .8); border-radius: 3px; background: rgba(255, 255, 255, .86); box-shadow: none; transform-origin: top left; }
        .flow-canvas-selection-actions.is-inline-selection:after { display: none; }
        .flow-canvas-selection-actions.is-inline-selection .btn { width: 17px; height: 17px; font-size: 10px; line-height: 17px; }
        .flow-canvas-selection-actions.is-inline-selection .flow-canvas-delete { color: #a16207; }
        .flow-canvas-selection-actions.is-edge-selection { display: block; width: min(330px, calc(100% - 24px)); padding: 6px; border-color: #b8cfca; background: rgba(252, 255, 254, .98); }
        .flow-canvas-edge-relation { display: flex; flex-direction: column; min-width: 0; color: #36504e; font-size: 11px; }
        .flow-canvas-edge-node { display: flex; align-items: center; min-width: 0; height: 25px; gap: 7px; padding: 0 8px; border: 1px solid #d8e7e3; border-radius: 3px; background: #f5faf8; }
        .flow-canvas-edge-node.successor { background: #fbfdfc; }
        .flow-canvas-edge-node .flow-canvas-edge-role { flex: 0 0 28px; color: #718096; font-size: 10px; font-weight: 600; }
        .flow-canvas-edge-node .flow-canvas-edge-name { min-width: 0; overflow: hidden; color: #36504e; font-weight: 500; text-overflow: ellipsis; white-space: nowrap; }
        .flow-canvas-edge-connector { position: relative; display: flex; align-items: center; height: 22px; padding-left: 19px; }
        .flow-canvas-edge-connector:before { position: absolute; top: 0; left: 22px; height: 7px; border-left: 1px solid #94b7b0; content: ''; }
        .flow-canvas-edge-connector > .fa { color: #216f6b; font-size: 14px; }
        .flow-canvas-edge-connector .flow-canvas-delete { width: 24px; height: 24px; margin-left: auto; border: 1px solid transparent; color: #b45309; }
        .flow-canvas-edge-connector .flow-canvas-delete:hover { border-color: #fed7aa; background: #fff7ed; color: #c2410c; }
        .flow-canvas-selection-actions .btn[disabled] { cursor: not-allowed; opacity: .55; }
        #flowCanvas { display: block; background-color: #fff; background-image: linear-gradient(#e5efec 1px, transparent 1px), linear-gradient(90deg, #e5efec 1px, transparent 1px); background-size: 24px 24px; }
        .flow-edge { fill: none; stroke: #94a3b8; stroke-width: 2.5; cursor: pointer; stroke-linejoin: round; stroke-linecap: round; transition: opacity .12s, stroke-width .12s; }
        .flow-edge.flow-edge-external { stroke-width: 3.2; }
        .flow-edge-halo { fill: none; stroke: #fff; stroke-width: 7; stroke-linejoin: round; stroke-linecap: round; pointer-events: none; }
        .flow-edge.flow-edge-internal { stroke-width: 1.7; stroke-dasharray: 5 3; }
        .flow-edge.flow-edge-muted { opacity: .14; stroke-width: 1; }
        .flow-edge:hover { stroke: #5b8783 !important; stroke-width: 3; stroke-dasharray: none; }
        .flow-edge.selected { stroke: #7c3aed !important; stroke-width: 3.2; stroke-dasharray: none; }
        .flow-node { cursor: move; }
        .flow-node .node-body { fill: #fff; stroke: #64748b; stroke-width: 1.5; transition: fill .15s ease, stroke .15s ease, stroke-width .15s ease; }
        .flow-node .node-header { fill: #f8fafc; pointer-events: none; }
        .flow-node:hover .node-body { stroke: #5b8783; stroke-width: 2; }
        .flow-node.selected .node-body { stroke: #7c3aed; stroke-width: 2.5; }
        .flow-node text { pointer-events: none; font-size: 12px; fill: #263238; }
        .flow-node .node-name { font-size: 11px; font-weight: 600; }
        .flow-node .node-status-strip { pointer-events: none; }
        .flow-node .node-status-badge { pointer-events: none; }
        .flow-node .node-status-label { fill: #fff; font-size: 10px; font-weight: 600; pointer-events: none; }
        .flow-node .node-turn-date { fill: #536d69; font-size: 10px; pointer-events: none; }
        .flow-node.selected .node-header { fill: #ede9fe !important; }
        .flow-node.selected .flow-port-output { stroke: #7c3aed; }
        .flow-node.status-wait .node-body { fill: #f8fafc; stroke: #94a3b8; }
        .flow-node.status-wait .node-header { fill: #f1f5f9; }
        .flow-node.status-dispatched .node-body { fill: #ecfeff; stroke: #0891b2; }
        .flow-node.status-dispatched .node-header { fill: #cffafe; }
        .flow-node.status-running .node-body { fill: #eff6ff; stroke: #2563eb; }
        .flow-node.status-running .node-header { fill: #dbeafe; }
        .flow-node.status-complete .node-body { fill: #f0fdf4; stroke: #16a34a; }
        .flow-node.status-complete .node-header { fill: #dcfce7; }
        .flow-node.status-exception .node-body { fill: #fef2f2; stroke: #dc2626; }
        .flow-node.status-exception .node-header { fill: #fee2e2; }
        .flow-node.status-skipped .node-body { fill: #faf5ff; stroke: #7c3aed; }
        .flow-node.status-skipped .node-header { fill: #f3e8ff; }
        .flow-node.status-stopping .node-body { fill: #fff7ed; stroke: #d97706; }
        .flow-node.status-stopping .node-header { fill: #ffedd5; }
        .flow-node.status-stopped .node-body { fill: #f1f5f9; stroke: #64748b; }
        .flow-node.status-stopped .node-header { fill: #e2e8f0; }
        .flow-node.status-uninitialized .node-body { fill: #f8fafc; stroke: #475569; }
        .flow-node.status-uninitialized .node-header { fill: #f1f5f9; }
        .flow-node.status-dispatched .node-status { fill: #0e7490; }
        .flow-node.status-running .node-status { fill: #1d4ed8; }
        .flow-node.status-complete .node-status { fill: #15803d; }
        .flow-node.status-exception .node-status { fill: #b91c1c; }
        .flow-node.status-skipped .node-status { fill: #6d28d9; }
        .flow-node.status-stopping .node-status { fill: #a16207; }
        .flow-node.status-stopped .node-status { fill: #475569; }
        .flow-node.status-uninitialized .node-status { fill: #475569; }
        .flow-port { stroke-width: 2; }
        .flow-port-output { fill: #fff; stroke: #216f6b; cursor: crosshair; }
        .flow-port-output.dependency-source { fill: #ede9fe; stroke: #7c3aed; stroke-width: 2.5; }
        .flow-canvas-wrap.is-connecting [data-flow-target] { cursor: cell; }
        .flow-canvas-wrap.is-connecting [data-flow-target].dependency-unavailable { cursor: not-allowed; }
        .flow-port-hit { fill: transparent; }
        .flow-port-output-hit { cursor: crosshair; }
        .flow-edge-preview { fill: none; stroke: #216f6b; stroke-width: 2; stroke-dasharray: 6 4; pointer-events: none; }
        .flow-node.dependency-target .node-body { stroke: #216f6b; stroke-width: 2.5; }
        .flow-group-box.dependency-target:not(.selected) { stroke: #216f6b; stroke-width: 2.5; stroke-dasharray: none; }
        .flow-group-box { fill: #f8fafc; stroke: #94a3b8; stroke-width: 1.5; stroke-dasharray: 6 4; cursor: move; }
        .flow-group-box.has-external-dependency { stroke-width: 2.2; stroke-dasharray: none; }
        .flow-group-box.selected { fill: #f5f3ff; stroke: #7c3aed; stroke-width: 3; stroke-dasharray: none; }
        .flow-group-header { fill: #f1f5f9; pointer-events: none; }
        .flow-group-header.selected { fill: #ede9fe; }
        .flow-group-label { font-size: 12px; font-weight: 600; fill: #315b58; pointer-events: none; }
        .flow-group-collapse { font-size: 12px; font-weight: 600; fill: #315b58; pointer-events: none; }
        .flow-group-collapse-hit { fill: transparent; cursor: pointer; }
        .flow-group-count { font-size: 10px; fill: #5d7c77; pointer-events: none; }
        .flow-group-dependency-count { font-size: 10px; fill: #216f6b; pointer-events: none; }
        .flow-group-box.status-wait { fill: #f8fafc; stroke: #94a3b8; }
        .flow-group-header.status-wait { fill: #f1f5f9; }
        .flow-group-box.status-dispatched { fill: #ecfeff; stroke: #0891b2; }
        .flow-group-box.status-running { fill: #eff6ff; stroke: #2563eb; }
        .flow-group-box.status-complete { fill: #f0fdf4; stroke: #16a34a; }
        .flow-group-box.status-exception { fill: #fef2f2; stroke: #dc2626; }
        .flow-group-box.status-skipped { fill: #faf5ff; stroke: #7c3aed; }
        .flow-group-box.status-stopping { fill: #fff7ed; stroke: #d97706; }
        .flow-group-box.status-stopped { fill: #f1f5f9; stroke: #64748b; }
        .flow-group-header.status-dispatched { fill: #cffafe; }
        .flow-group-header.status-running { fill: #dbeafe; }
        .flow-group-header.status-complete { fill: #dcfce7; }
        .flow-group-header.status-exception { fill: #fee2e2; }
        .flow-group-header.status-skipped { fill: #f3e8ff; }
        .flow-group-header.status-stopping { fill: #ffedd5; }
        .flow-group-header.status-stopped { fill: #e2e8f0; }
        .flow-group-box.status-uninitialized { fill: #f8fafc; stroke: #475569; }
        .flow-group-header.status-uninitialized { fill: #f1f5f9; }
        .flow-edge.status-wait { stroke: #94a3b8; }
        .flow-edge.status-dispatched { stroke: #0891b2; }
        .flow-edge.status-running { stroke: #2563eb; }
        .flow-edge.status-complete { stroke: #16a34a; }
        .flow-edge.status-exception { stroke: #dc2626; }
        .flow-edge.status-skipped { stroke: #7c3aed; }
        .flow-edge.status-stopping { stroke: #d97706; }
        .flow-edge.status-stopped { stroke: #64748b; }
        .flow-edge.status-uninitialized { stroke: #475569; }
        .flow-stage-divider { stroke: #c8dce9; stroke-width: 1; stroke-dasharray: 3 4; pointer-events: none; }
        .flow-stage-label { font-size: 10px; fill: #6a879a; pointer-events: none; }
        .flow-batch-empty { padding: 28px 10px; color: #8a96a3; text-align: center; }
        .flow-muted { color: #8a96a3; font-size: 11px; font-weight: normal; }
        .flow-canvas-tools { display: flex; align-items: center; gap: 4px; }
        .flow-canvas-tools .btn { padding: 1px 6px; line-height: 20px; transition: background-color .16s, border-color .16s, color .16s; }
        .flow-canvas-tools .btn:hover { border-color: #3d8882; background: #eaf6f3; color: #216f6b; }
        .flow-canvas-tools .btn.is-active { border-color: #175d59; background: #216f6b; color: #fff; }
        .flow-canvas-title { min-width: 0; flex: 1; display: flex; align-items: center; gap: 14px; }
        .flow-canvas-title-text { flex: 0 0 auto; min-width: 0; white-space: nowrap; }
        .flow-canvas-search { position: relative; flex: 1 1 440px; min-width: 300px; max-width: 560px; }
        .flow-canvas-search .form-control { height: 28px; padding: 4px 30px 4px 10px; border-color: #b9d0cb; border-radius: 4px; background: #f9fcfb; box-shadow: inset 0 1px 1px rgba(30, 55, 57, .03); color: #263b3d; font-size: 12px; transition: border-color .16s, box-shadow .16s, background-color .16s; }
        .flow-canvas-search .form-control:hover { border-color: #83afa8; background: #fff; }
        .flow-canvas-search .form-control:focus { border-color: #216f6b; background: #fff; box-shadow: 0 0 0 2px rgba(33, 111, 107, .14); }
        .flow-canvas-search .fa-search { position: absolute; top: 8px; right: 10px; color: #52716c; font-size: 12px; pointer-events: none; }
        .flow-canvas-search-results { position: absolute; z-index: 9; top: calc(100% + 5px); right: 0; left: 0; max-height: 250px; overflow: auto; border: 1px solid #b9d0cb; border-radius: 4px; background: #fff; box-shadow: 0 8px 20px rgba(25, 49, 46, .16); }
        .flow-canvas-search-results:empty { display: none; }
        .flow-canvas-search-results .flow-batch-empty { padding: 12px 8px; }
        .flow-canvas-search-result { display: flex; align-items: center; gap: 8px; width: 100%; min-height: 38px; padding: 7px 10px; border: 0; border-bottom: 1px solid #edf0f3; background: #fff; color: #334155; font-size: 12px; text-align: left; }
        .flow-canvas-search-result > .fa { width: 14px; color: #52716c; text-align: center; }
        .flow-canvas-search-result:hover { background: #eef8f5; color: #175d59; }
        .flow-canvas-search-result:last-child { border-bottom: 0; }
        .flow-canvas-search-result span { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
        .flow-canvas-search-result small { margin-left: auto; padding-left: 10px; color: #78918d; font-size: 11px; }
        .flow-zoom-value { min-width: 42px; text-align: center; color: #64748b; font-size: 11px; }
        .flow-status-legend { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; padding: 4px 10px; border-bottom: 1px solid #edf0f3; background: #fff; color: #64748b; font-size: 10px; }
        .flow-status-legend span { display: inline-flex; align-items: center; gap: 4px; }
        .flow-status-separator { width: 1px; height: 14px; margin: 0 2px; background: #dfe4ea; }
        .flow-status-dot { width: 7px; height: 7px; display: inline-block; border-radius: 50%; }
        .flow-status-dot.wait { background: #94a3b8; }.flow-status-dot.dispatched { background: #0891b2; }.flow-status-dot.running { background: #2563eb; }.flow-status-dot.complete { background: #16a34a; }.flow-status-dot.exception { background: #dc2626; }.flow-status-dot.skipped { background: #7c3aed; }.flow-status-dot.stopping { background: #d97706; }.flow-status-dot.stopped { background: #64748b; }.flow-status-dot.uninitialized { background: #475569; }
        .flow-node:hover .node-body { fill: #f8fcfb; stroke: #5b8783; stroke-width: 2; }
        .flow-node.selected .node-body { fill: #f5f3ff; stroke: #7c3aed; stroke-width: 2.5; }
        .flow-group-box:not(.selected):hover { fill: #f8fcfb; stroke: #5b8783; stroke-width: 2; stroke-dasharray: none; }
        .flow-group-box:not(.selected):hover + .flow-group-header { fill: #edf7f4; }
        .flow-group-box.selected { fill: #f5f3ff; stroke: #7c3aed; stroke-width: 3; }
        .flow-group-header.selected { fill: #ede9fe; }
        @media (max-width: 1180px) { .flow-toolbar { align-items: flex-start; flex-direction: column; gap: 8px; } .flow-actions { width: 100%; justify-content: flex-start; } }
        @media (max-width: 1100px) { .flow-canvas-search { flex-basis: 320px; min-width: 220px; } .flow-canvas-title .flow-muted { display: none; } }
        @media (max-width: 780px) { html, body { overflow: auto; } .flow-page { height: auto; min-height: 100vh; } .flow-main { grid-template-columns: 1fr; } .flow-panel { min-height: 240px; } .flow-panel-title { flex-wrap: wrap; } .flow-canvas-title { flex-basis: 100%; flex-wrap: wrap; gap: 6px; } .flow-canvas-search { flex-basis: 100%; min-width: 0; max-width: none; } .flow-canvas-tools { margin-left: auto; } .flow-canvas-wrap { min-height: 420px; } .flow-actions { gap: 5px; } }
    </style>
</head>
<body>
<div class="flow-page">
    <input type="hidden" id="workId" value="${workId}">
    <div class="flow-toolbar">
        <div class="flow-work-meta">
            <div class="flow-title"><i class="fa fa-sitemap"></i> 作业配置</div>
            <div class="flow-work-meta-item"><span>作业名称</span><strong id="flowWorkName">-</strong></div>
            <div class="flow-work-meta-item"><span>作业 ID</span><strong id="flowWorkId">${workId}</strong></div>
            <div class="flow-work-meta-item"><span>作业类型</span><strong id="flowWorkType">-</strong></div>
            <div class="flow-work-meta-item"><span>翻牌时间</span><strong id="flowWorkTurnDate">-</strong></div>
            <div class="flow-work-meta-item status"><span>运行状态</span><strong id="flowRunStatus">-</strong></div>
        </div>
        <div class="flow-actions">
            <div class="flow-action-cluster">
                <button type="button" class="btn btn-sm flow-action-primary" id="addNodeAction"><i class="fa fa-plus"></i> 增加节点</button>
                <button type="button" class="btn btn-sm" id="addGroupAction"><i class="fa fa-folder-o"></i> 增加组</button>
                <div class="btn-group">
                    <button type="button" class="btn btn-sm dropdown-toggle" data-toggle="dropdown"><i class="fa fa-play-circle"></i> 运行 <span class="caret"></span></button>
                    <ul class="dropdown-menu dropdown-menu-right">
                        <li id="flowRerun"><a href="javascript:void(0);"><i class="fa fa-history"></i> 重跑作业</a></li>
                        <li id="flowStopRun"><a href="javascript:void(0);"><i class="fa fa-stop"></i> 停止当前批次</a></li>
                        <li id="flowRecover"><a href="javascript:void(0);"><i class="fa fa-repeat"></i> 恢复重跑</a></li>
                    </ul>
                </div>
                <button type="button" class="btn btn-sm" id="viewFlowLog" title="查看当前作业的所有运行日志"><i class="fa fa-file-text-o"></i> 运行日志</button>
            </div>
            <div class="flow-action-cluster">
                <div class="btn-group">
                    <button type="button" class="btn btn-sm dropdown-toggle" data-toggle="dropdown"><i class="fa fa-clock-o"></i> 调度 <span class="caret"></span></button>
                    <ul class="dropdown-menu dropdown-menu-right">
                        <li id="flowStartSchedule"><a href="javascript:void(0);"><i class="fa fa-play"></i> 启动定时</a></li>
                        <li id="flowStopSchedule"><a href="javascript:void(0);"><i class="fa fa-pause"></i> 停止定时</a></li>
                    </ul>
                </div>
                <div class="btn-group">
                    <button type="button" class="btn btn-sm dropdown-toggle" data-toggle="dropdown"><i class="fa fa-file-excel-o"></i> 流程配置 <span class="caret"></span></button>
                    <ul class="dropdown-menu dropdown-menu-right">
                        <li id="downloadDependencyTemplate"><a href="javascript:void(0);" title="每个 Sheet 均包含自动跳过的示例行"><i class="fa fa-file-o"></i> 下载配置模板</a></li>
                        <li id="importDependencies"><a href="javascript:void(0);"><i class="fa fa-upload"></i> 导入流程配置</a></li>
                        <li id="exportDependencies"><a href="javascript:void(0);"><i class="fa fa-download"></i> 导出流程配置</a></li>
                    </ul>
                </div>
            </div>
            <button type="button" class="btn btn-sm flow-action-icon" id="refreshAndLayout" title="刷新并自动布局"><i class="fa fa-refresh"></i></button>
        </div>
    </div>

    <div class="flow-main">
        <section class="flow-panel">
            <div class="flow-panel-title"><span class="flow-canvas-title"><span class="flow-canvas-title-text">流程图 <span class="flow-muted">拖动节点调整位置，从节点右侧圆点拖到目标节点创建依赖</span></span><span class="flow-canvas-search"><input type="search" class="form-control" id="canvasObjectSearch" placeholder="搜索节点或组"><i class="fa fa-search"></i><span class="flow-canvas-search-results" id="canvasObjectSearchResults"></span></span></span><span class="flow-canvas-tools"><button type="button" class="btn btn-default btn-xs" id="zoomOut" title="缩小">−</button><span class="flow-zoom-value" id="zoomValue" title="Ctrl + 滚轮缩放">100%</span><button type="button" class="btn btn-default btn-xs" id="zoomIn" title="放大">+</button><button type="button" class="btn btn-default btn-xs" id="fitCanvas" title="适配画布"><i class="fa fa-arrows-alt"></i></button></span></div>
            <div class="flow-status-legend"><span><i class="flow-status-dot exception"></i>执行异常</span><span><i class="flow-status-dot stopping"></i>停止中</span><span><i class="flow-status-dot running"></i>进行中</span><span><i class="flow-status-dot dispatched"></i>已下发</span><span><i class="flow-status-dot wait"></i>待执行</span><i class="flow-status-separator"></i><span><i class="flow-status-dot complete"></i>执行完毕</span><span><i class="flow-status-dot skipped"></i>已跳过</span><span><i class="flow-status-dot stopped"></i>已停止</span><span><i class="flow-status-dot uninitialized"></i>未初始化</span></div>
            <div class="flow-canvas-wrap" id="canvasWrap"><div class="flow-canvas-selection-actions" id="flowCanvasSelectionActions" hidden></div><div class="flow-node-tooltip" id="flowNodeTooltip"></div><svg id="flowCanvas" viewBox="0 0 1200 680" xmlns="http://www.w3.org/2000/svg"></svg></div>
        </section>
    </div>
</div>

<@netCommon.commonScript />
<input type="file" id="dependencyImportFile" accept=".xlsx,.xls" style="display:none">
<script>
    (function () {
        var workId = $('#workId').val();
        var groups = [], nodes = [], dependencies = [], groupMap = {}, nodeMap = {}, groupById = {}, nodeById = {}, nodesByGroup = {}, dependenciesByEndpoint = {}, dependencyById = {}, groupDependencyStats = {}, flowLoadVersion = 0, workTurnDate = '', workRuntime = {};
        var positions = {}, selected = null, dragState = null, dependencyDrag = null, runtimeEventSource = null, pendingRuntimeUpdate = null, hoveredNodeId = null, dependencyPointer = null, dependencyAutoScrollFrame = null;
        var canvasSize = {width: 1400, height: 800}, zoom = 1, collapsedGroups = {}, renderFrame = null, groupStageInfo = {}, groupFrames = {}, needsDependencyLayout = true;
        var nodeWidth = 244, nodeMinHeight = 72, nodeNameLineLength = 20, groupGapX = 900;
        var groupTopPadding = 50, groupBottomPadding = 28;
        var maxDependencySearchResults = 20;
        var svg = document.getElementById('flowCanvas');
        var canvasWrap = document.getElementById('canvasWrap');
        var nodeTooltip = document.getElementById('flowNodeTooltip');
        var flowRunStatusNames = {
            <#list runStatusEnum as status>
            ${status.code}: '${status.value}'<#if status_has_next>,</#if>
            </#list>
        };

        function escapeHtml(value) { return $('<div>').text(value == null ? '' : value).html(); }
        function nodeRetryText(node) { return node.currentRetryTimes === -1 ? '无限' : (node.currentRetryTimes == null ? '-' : String(node.currentRetryTimes)); }
        function errorStrategyText(value) { var map = {stop: '停止整个流程', skip: '跳过继续', retry: '重试'}; return map[String(value || '').toLowerCase()] || '-'; }
        function nodeTooltipContent(node) { return '<div class="flow-node-tooltip-title">' + escapeHtml(node.nodeName || node.nodeId) + '</div><dl class="flow-node-tooltip-meta"><dt>节点 ID</dt><dd>' + escapeHtml(node.nodeId) + '</dd><dt>节点类型</dt><dd>' + escapeHtml(node.nodeTypeName || '-') + '</dd><dt>数据源</dt><dd>' + escapeHtml(node.dbTypeName || '-') + '</dd><dt>运行状态</dt><dd>' + escapeHtml(statusText(node.nodeRunStatus)) + '</dd><dt>翻牌日期</dt><dd>' + escapeHtml(turnDateText(node.turnDate || workTurnDate)) + '</dd><dt>失败策略</dt><dd>' + escapeHtml(errorStrategyText(node.errorStrategy)) + '</dd><dt>剩余重试</dt><dd>' + escapeHtml(nodeRetryText(node)) + '</dd></dl>'; }
        function positionNodeTooltip(event) { if (!nodeTooltip || nodeTooltip.style.display === 'none') return; var wrapRect = canvasWrap.getBoundingClientRect(), left = event.clientX - wrapRect.left + canvasWrap.scrollLeft + 14, top = event.clientY - wrapRect.top + canvasWrap.scrollTop + 14, maxLeft = canvasWrap.scrollLeft + canvasWrap.clientWidth - nodeTooltip.offsetWidth - 8, maxTop = canvasWrap.scrollTop + canvasWrap.clientHeight - nodeTooltip.offsetHeight - 8; if (left > maxLeft) left = Math.max(canvasWrap.scrollLeft + 8, event.clientX - wrapRect.left + canvasWrap.scrollLeft - nodeTooltip.offsetWidth - 14); if (top > maxTop) top = Math.max(canvasWrap.scrollTop + 8, event.clientY - wrapRect.top + canvasWrap.scrollTop - nodeTooltip.offsetHeight - 14); nodeTooltip.style.left = left + 'px'; nodeTooltip.style.top = top + 'px'; }
        function showNodeTooltip(node, event) { if (dragState || dependencyDrag) return; hoveredNodeId = node.nodeId; nodeTooltip.innerHTML = nodeTooltipContent(node); nodeTooltip.style.display = 'block'; positionNodeTooltip(event); }
        function hideNodeTooltip() { hoveredNodeId = null; nodeTooltip.style.display = 'none'; }
        function statusText(value) { return value == null ? flowRunStatusNames[0] : (flowRunStatusNames[value] || value); }
        function statusClass(value) { var map = {0: 'wait', 1: 'running', 2: 'complete', 3: 'exception', 4: 'dispatched', 5: 'skipped', 6: 'stopped', 7: 'stopping', 8: 'uninitialized'}; return map[value] || 'wait'; }
        function statusColor(name) { var map = {wait: '#94a3b8', dispatched: '#0891b2', running: '#2563eb', complete: '#16a34a', exception: '#dc2626', skipped: '#7c3aed', stopping: '#d97706', stopped: '#64748b', uninitialized: '#475569'}; return map[name] || map.wait; }
        function turnDateText(value) { var text = String(value || ''); return text ? text.substring(0, 10) : '-'; }
        function endpointKey(type, id) { return type + ':' + id; }
        function endpointLabel(type, id) { var map = type === 'GROUP' ? groupMap : nodeMap, name = map[id] || id; return type === 'GROUP' ? '[组] ' + name : '[节点] ' + name + '（' + id + '）'; }
        function endpointRelationLabel(type, id) { var map = type === 'GROUP' ? groupMap : nodeMap; return (type === 'GROUP' ? '[组] ' : '[节点] ') + (map[id] || id); }
        function canvasSearchLabel(type, id) { return type === 'GROUP' ? '[组] ' + (groupMap[id] || id) + '（' + id + '）' : endpointLabel(type, id); }
        function objectExists(type, id) { return !!(type === 'GROUP' ? groupMap[id] : nodeMap[id]); }
        function rebuildElementIndexes() {
            groupMap = {}; nodeMap = {}; groupById = {}; nodeById = {}; nodesByGroup = {};
            groups.forEach(function (group) { groupMap[group.groupId] = group.groupName; groupById[group.groupId] = group; nodesByGroup[group.groupId] = []; });
            nodes.forEach(function (node) { nodeMap[node.nodeId] = node.nodeName; nodeById[node.nodeId] = node; (nodesByGroup[node.groupId] = nodesByGroup[node.groupId] || []).push(node); });
        }
        function rebuildDependencyIndexes() {
            dependenciesByEndpoint = {}; dependencyById = {}; groupDependencyStats = {};
            groups.forEach(function (group) { groupDependencyStats[group.groupId] = {crossCount: 0, relatedCount: 0, hasExternalDependency: false}; });
            dependencies.forEach(function (dependency) {
                dependencyById[dependency.dependencyId] = dependency;
                [endpointKey(dependency.predecessorType, dependency.predecessorId), endpointKey(dependency.successorType, dependency.successorId)].forEach(function (key, index, keys) {
                    if (index && key === keys[0]) return;
                    (dependenciesByEndpoint[key] = dependenciesByEndpoint[key] || []).push(dependency);
                });
                var predecessorGroupId = endpointGroup(dependency.predecessorType, dependency.predecessorId), successorGroupId = endpointGroup(dependency.successorType, dependency.successorId);
                [predecessorGroupId, successorGroupId].forEach(function (groupId, index, groupIds) {
                    if (!groupId || (index && groupId === groupIds[0])) return;
                    var stats = groupDependencyStats[groupId] || (groupDependencyStats[groupId] = {crossCount: 0, relatedCount: 0, hasExternalDependency: false});
                    stats.relatedCount++;
                    if (predecessorGroupId !== successorGroupId) { stats.crossCount++; stats.hasExternalDependency = true; }
                });
            });
        }
        function groupNodes(groupId) { return nodesByGroup[groupId] || []; }
        function svgPoint(event) { var rect = svg.getBoundingClientRect(); return {x: (event.clientX - rect.left) * canvasSize.width / rect.width, y: (event.clientY - rect.top) * canvasSize.height / rect.height}; }
        function makeSvg(tag, attrs) { var element = document.createElementNS('http://www.w3.org/2000/svg', tag); Object.keys(attrs || {}).forEach(function (key) { element.setAttribute(key, attrs[key]); }); return element; }
        function ensurePosition(key, index) { if (!positions[key]) positions[key] = {x: 70 + (index % 4) * 280, y: 90 + Math.floor(index / 4) * 104}; return positions[key]; }
        function truncateText(value, maxLength) { var text = String(value == null ? '' : value); return text.length > maxLength ? text.substring(0, Math.max(1, maxLength - 3)) + '...' : (text || '-'); }
        function nodeNameLines(node) { var value = String(node.nodeName || node.nodeId || '-'); if (value.length <= nodeNameLineLength) return [value]; return [value.substring(0, nodeNameLineLength), truncateText(value.substring(nodeNameLineLength), nodeNameLineLength)]; }
        function nodeHeightOf() { return nodeMinHeight; }
        function endpointGroup(type, id) { if (type === 'GROUP') return id; var node = nodeById[id]; return node ? node.groupId : null; }
        function endpointRunStatus(type, id) { var item = type === 'GROUP' ? groupById[id] : nodeById[id]; return item ? (type === 'GROUP' ? item.runStatus : item.nodeRunStatus) : null; }
        function dependencyStatusClass(item) { var successorStatus = statusClass(endpointRunStatus(item.successorType, item.successorId)); if (successorStatus !== 'wait') return successorStatus; return statusClass(endpointRunStatus(item.predecessorType, item.predecessorId)); }
        function groupDependencyInfo(groupId) { return groupDependencyStats[groupId] || {crossCount: 0, relatedCount: 0, hasExternalDependency: false}; }
        function isRelatedToSelected(item) { if (!selected) return false; if (selected.kind === 'GROUP') return endpointGroup(item.predecessorType, item.predecessorId) === selected.id || endpointGroup(item.successorType, item.successorId) === selected.id; return (selected.kind === item.predecessorType && selected.id === item.predecessorId) || (selected.kind === item.successorType && selected.id === item.successorId); }
        function isHighlightedEdge(item) { return !!selected && ((selected.kind === 'EDGE' && selected.id === item.dependencyId) || (selected.kind !== 'EDGE' && isRelatedToSelected(item))); }
        function isSelectedEdgeEndpoint(type, id) {
            if (!selected || selected.kind !== 'EDGE') return false;
            var edge = dependencyById[selected.id];
            return !!edge && ((edge.predecessorType === type && edge.predecessorId === id)
                || (edge.successorType === type && edge.successorId === id));
        }
        function isSelectedGroupNode(node) { return !!selected && selected.kind === 'GROUP' && selected.id === node.groupId; }
        function dependencyTargetAtPoint(clientX, clientY) {
            var source = document.elementFromPoint(clientX, clientY);
            var target = source && source.closest ? source.closest('[data-flow-target]') : null;
            return target && (target.dataset.type === 'NODE' || target.dataset.type === 'GROUP') ? target : null;
        }
        function dependencyTarget(event) { return dependencyTargetAtPoint(event.clientX, event.clientY); }
        function isSameDependencyEndpoint(target) { return !!dependencyDrag && target && target.dataset.type === dependencyDrag.type && target.dataset.id === dependencyDrag.id; }
        function sameDependencyPort(element, target) { return element.dataset.type === target.dataset.type && element.dataset.id === target.dataset.id; }
        function setDependencyTarget(target) {
            $('[data-flow-target].dependency-target').removeClass('dependency-target');
            if (target && !isSameDependencyEndpoint(target)) $('[data-flow-target]').filter(function () { return sameDependencyPort(this, target); }).addClass('dependency-target');
        }
        function markDependencyPorts() {
            $('[data-flow-output], [data-flow-target]').removeClass('dependency-source dependency-target dependency-unavailable');
            $('[data-flow-output]').filter(function () { return this.dataset.type === dependencyDrag.type && this.dataset.id === dependencyDrag.id; }).addClass('dependency-source');
            $('[data-flow-target]').filter(function () { return this.dataset.type === dependencyDrag.type && this.dataset.id === dependencyDrag.id; }).addClass('dependency-unavailable');
        }
        function dependencyScrollStep(distance) { return Math.max(2, Math.ceil((56 - distance) / 56 * 16)); }
        function updateDependencyAutoScroll(event) {
            dependencyPointer = {clientX: event.clientX, clientY: event.clientY};
            if (dependencyAutoScrollFrame == null) dependencyAutoScrollFrame = window.requestAnimationFrame(runDependencyAutoScroll);
        }
        function runDependencyAutoScroll() {
            dependencyAutoScrollFrame = null;
            if (!dependencyDrag || !dependencyPointer) return;
            var rect = canvasWrap.getBoundingClientRect(), pointer = dependencyPointer, threshold = 56, deltaX = 0, deltaY = 0;
            if (pointer.clientX >= rect.left && pointer.clientX <= rect.right) {
                if (pointer.clientX - rect.left < threshold) deltaX = -dependencyScrollStep(pointer.clientX - rect.left);
                else if (rect.right - pointer.clientX < threshold) deltaX = dependencyScrollStep(rect.right - pointer.clientX);
            }
            if (pointer.clientY >= rect.top && pointer.clientY <= rect.bottom) {
                if (pointer.clientY - rect.top < threshold) deltaY = -dependencyScrollStep(pointer.clientY - rect.top);
                else if (rect.bottom - pointer.clientY < threshold) deltaY = dependencyScrollStep(rect.bottom - pointer.clientY);
            }
            if (deltaX || deltaY) {
                var previousLeft = canvasWrap.scrollLeft, previousTop = canvasWrap.scrollTop;
                canvasWrap.scrollLeft += deltaX;
                canvasWrap.scrollTop += deltaY;
                if (canvasWrap.scrollLeft !== previousLeft || canvasWrap.scrollTop !== previousTop) updateDependencyDrag(pointer, true);
            }
            dependencyAutoScrollFrame = window.requestAnimationFrame(runDependencyAutoScroll);
        }
        function stopDependencyAutoScroll() {
            dependencyPointer = null;
            if (dependencyAutoScrollFrame != null) window.cancelAnimationFrame(dependencyAutoScrollFrame);
            dependencyAutoScrollFrame = null;
        }
        function clearDependencyDrag() {
            $('[data-flow-output], [data-flow-target]').removeClass('dependency-source dependency-target dependency-unavailable');
            if (dependencyDrag && dependencyDrag.preview && dependencyDrag.preview.parentNode) {
                dependencyDrag.preview.parentNode.removeChild(dependencyDrag.preview);
            }
            dependencyDrag = null;
            stopDependencyAutoScroll();
            canvasWrap.classList.remove('is-connecting');
            hideNodeTooltip();
            renderCanvasSelectionActions();
        }
        function updateDependencyDrag(event, skipAutoScroll) {
            if (!dependencyDrag) return;
            if (Math.abs(event.clientX - dependencyDrag.startX) + Math.abs(event.clientY - dependencyDrag.startY) > 5) dependencyDrag.moved = true;
            var start = endpointPoint(dependencyDrag.type, dependencyDrag.id, 'from');
            dependencyDrag.preview.setAttribute('d', edgeGeometry(start, svgPoint(event), 0).path);
            var target = dependencyTarget(event);
            setDependencyTarget(target);
            if (!skipAutoScroll) updateDependencyAutoScroll(event);
        }
        function startDependencyDrag(event, type, id) {
            if (event.button !== undefined && event.button !== 0) return;
            event.preventDefault();
            event.stopPropagation();
            if (dependencyDrag && dependencyDrag.locked && dependencyDrag.type === type && dependencyDrag.id === id) {
                clearDependencyDrag();
                return;
            }
            clearDependencyDrag();
            var start = endpointPoint(type, id, 'from');
            var preview = makeSvg('path', {d: edgeGeometry(start, svgPoint(event), 0).path, 'class': 'flow-edge-preview'});
            svg.appendChild(preview);
            dependencyDrag = {type: type, id: id, preview: preview, startX: event.clientX, startY: event.clientY, moved: false, locked: false};
            hideNodeTooltip();
            $('#flowCanvasSelectionActions').attr('hidden', true);
            canvasWrap.classList.add('is-connecting');
            markDependencyPorts();
            updateDependencyAutoScroll(event);
        }
        function appendDependencyOutput(container, type, id, outputX, centerY) {
            var outputAttributes = {'data-flow-output': 'true', 'data-type': type, 'data-id': id};
            var stopPortEvent = function (event) { event.stopPropagation(); };
            var outputHit = makeSvg('rect', $.extend({x: outputX - 2, y: centerY - 12, width: 14, height: 24, 'class': 'flow-port-hit flow-port-output-hit'}, outputAttributes));
            outputHit.addEventListener('pointerdown', function (event) { startDependencyDrag(event, type, id); });
            outputHit.addEventListener('click', stopPortEvent);
            container.appendChild(outputHit);
            var outputPort = makeSvg('circle', $.extend({cx: outputX, cy: centerY, r: 4, 'class': 'flow-port flow-port-output'}, outputAttributes));
            outputPort.appendChild(makeSvg('title', {})).textContent = '作为依赖起始端';
            outputPort.addEventListener('pointerdown', function (event) { startDependencyDrag(event, type, id); });
            outputPort.addEventListener('click', stopPortEvent);
            container.appendChild(outputPort);
        }
        function isDependencyPort(element) { return !!(element && element.closest && element.closest('[data-flow-output]')); }
        function startGroupDrag(event, groupId, bounds) {
            if (event.button !== undefined && event.button !== 0) return;
            var point = svgPoint(event), nodePositions = {};
            groupNodes(groupId).forEach(function (node, index) {
                var position = ensurePosition(endpointKey('NODE', node.nodeId), index);
                nodePositions[node.nodeId] = {x: position.x, y: position.y};
            });
            dragState = {kind: 'GROUP', id: groupId, startPoint: point, bounds: bounds, collapsed: !!collapsedGroups[groupId], nodePositions: nodePositions};
        }
        function updateGroupDrag(point) {
            var deltaX = Math.max(20 - dragState.bounds.x, point.x - dragState.startPoint.x), deltaY = Math.max(20 - dragState.bounds.y, point.y - dragState.startPoint.y);
            positions[endpointKey('GROUP', dragState.id)] = {x: Math.max(32, dragState.bounds.x + 12 + deltaX), y: Math.max(32, dragState.bounds.y + 12 + deltaY)};
            if (!dragState.collapsed) groupFrames[dragState.id] = {x: dragState.bounds.x + deltaX, y: dragState.bounds.y + deltaY, width: dragState.bounds.width, height: dragState.bounds.height};
            Object.keys(dragState.nodePositions).forEach(function (nodeId) {
                var position = dragState.nodePositions[nodeId];
                positions[endpointKey('NODE', nodeId)] = {x: Math.max(10, position.x + deltaX), y: Math.max(10, position.y + deltaY)};
            });
        }
        function expandGroupFrame(groupId) {
            var frame = groupFrames[groupId];
            if (!frame) return;
            groupNodes(groupId).forEach(function (node) {
                var position = positions[endpointKey('NODE', node.nodeId)];
                if (!position) return;
                frame.width = Math.max(frame.width, position.x + nodeWidth - frame.x + 20);
                frame.height = Math.max(frame.height, position.y + nodeHeightOf(node) - frame.y + groupBottomPadding);
            });
        }
        function updateNodeDrag(point) {
            var node = nodeById[dragState.id], group = node && groupById[node.groupId];
            if (!node || !group) return;
            var index = groups.indexOf(group), frame = groupBounds(group, index), position = {x: Math.max(frame.x + 10, point.x - dragState.dx), y: Math.max(frame.y + 38, point.y - dragState.dy)};
            positions[endpointKey('NODE', node.nodeId)] = position;
            expandGroupFrame(group.groupId);
        }
        function arrangeGroupNodes(groupNodes, baseX, baseY) { var colCount = Math.min(3, Math.max(1, Math.ceil(Math.sqrt(groupNodes.length || 1)))), rowHeights = [], rowY = [], totalHeight = 0; groupNodes.forEach(function (node, index) { var row = Math.floor(index / colCount); rowHeights[row] = Math.max(rowHeights[row] || 0, nodeHeightOf(node)); }); rowHeights.forEach(function (height, row) { rowY[row] = baseY + totalHeight; totalHeight += height + 14; }); groupNodes.forEach(function (node, index) { var col = index % colCount, row = Math.floor(index / colCount); positions[endpointKey('NODE', node.nodeId)] = {x: baseX + col * (nodeWidth + 14), y: rowY[row]}; }); return {height: Math.max(0, totalHeight - 14), colCount: colCount}; }
        function buildGroupStageInfo(groupNodes) { var nodeSet = {}, stages = {}, buckets = {}; groupNodes.forEach(function (node) { nodeSet[node.nodeId] = true; stages[node.nodeId] = 0; }); for (var pass = 0; pass < groupNodes.length; pass++) { dependencies.forEach(function (item) { if (item.predecessorType === 'NODE' && item.successorType === 'NODE' && nodeSet[item.predecessorId] && nodeSet[item.successorId]) stages[item.successorId] = Math.min(groupNodes.length, Math.max(stages[item.successorId], stages[item.predecessorId] + 1)); }); } groupNodes.forEach(function (node) { var stage = stages[node.nodeId] || 0; (buckets[stage] = buckets[stage] || []).push(node); }); var stageKeys = Object.keys(buckets).map(Number).sort(function (a, b) { return a - b; }), height = 0; stageKeys.forEach(function (stage) { var columnHeight = 0; buckets[stage].forEach(function (node) { columnHeight += nodeHeightOf(node) + 14; }); height = Math.max(height, Math.max(0, columnHeight - 14)); }); return {buckets: buckets, stages: stageKeys, width: Math.max(290, stageKeys.length * (nodeWidth + 54) - 54), height: height}; }
        function calculatedGroupBounds(group, index) {
            var groupNodeList = groupNodes(group.groupId);
            var groupPosition = positions[endpointKey('GROUP', group.groupId)], first = groupNodeList.length ? ensurePosition(endpointKey('NODE', groupNodeList[0].nodeId), index) : (groupPosition || {x: 70 + (index % 4) * groupGapX, y: 100 + Math.floor(index / 4) * 150}), minX = first.x, minY = first.y, maxX = first.x + nodeWidth, maxY = first.y + nodeMinHeight;
            groupNodeList.forEach(function (node) { var point = ensurePosition(endpointKey('NODE', node.nodeId), index), height = nodeHeightOf(node); minX = Math.min(minX, point.x); minY = Math.min(minY, point.y); maxX = Math.max(maxX, point.x + nodeWidth); maxY = Math.max(maxY, point.y + height); });
            return {x: Math.max(20, minX - 20), y: Math.max(20, minY - groupTopPadding), width: Math.max(290, maxX - minX + 40), height: Math.max(96, maxY - minY + groupTopPadding + groupBottomPadding)};
        }
        function groupBounds(group, index) {
            var groupPosition = positions[endpointKey('GROUP', group.groupId)];
            if (collapsedGroups[group.groupId]) { var collapsedFrame = groupFrames[group.groupId] || calculatedGroupBounds(group, index), collapsedPoint = groupPosition || {x: collapsedFrame.x + 12, y: collapsedFrame.y + 12}; return {x: Math.max(20, collapsedPoint.x - 12), y: Math.max(20, collapsedPoint.y - 12), width: Math.max(300, nodeWidth + 24), height: 54}; }
            if (!groupFrames[group.groupId]) groupFrames[group.groupId] = calculatedGroupBounds(group, index);
            return groupFrames[group.groupId];
        }
        function updateCanvasSize() { var maxX = 1200, maxY = 680; groups.forEach(function (group, index) { var bounds = groupBounds(group, index); maxX = Math.max(maxX, bounds.x + bounds.width + 80); maxY = Math.max(maxY, bounds.y + bounds.height + 80); }); nodes.forEach(function (node, index) { if (collapsedGroups[node.groupId]) return; var point = ensurePosition(endpointKey('NODE', node.nodeId), index); maxX = Math.max(maxX, point.x + nodeWidth + 80); maxY = Math.max(maxY, point.y + nodeHeightOf(node) + 80); }); canvasSize = {width: Math.ceil(maxX), height: Math.ceil(maxY)}; svg.setAttribute('viewBox', '0 0 ' + canvasSize.width + ' ' + canvasSize.height); svg.style.width = (canvasSize.width * zoom) + 'px'; svg.style.height = (canvasSize.height * zoom) + 'px'; $('#zoomValue').text(Math.round(zoom * 100) + '%'); positionCanvasSelectionActions(); }
        function indicateCanvasTool(button) { $(button).addClass('is-active'); window.setTimeout(function () { $(button).removeClass('is-active'); }, 180); }
        function layoutDefaults() {
            var columnBottoms = [0, 0, 0, 0]; groupStageInfo = {}; groupFrames = {};
            groups.forEach(function (group) { var groupNodeList = groupNodes(group.groupId), column = columnBottoms.indexOf(Math.min.apply(Math, columnBottoms)), baseY = Math.max(110, columnBottoms[column] + 24), baseX = 70 + column * groupGapX, arrangement = arrangeGroupNodes(groupNodeList, baseX, baseY), collapsed = !!collapsedGroups[group.groupId];
                positions[endpointKey('GROUP', group.groupId)] = {x: baseX, y: baseY};
                columnBottoms[column] = baseY + (collapsed ? 82 : Math.max(125, arrangement.height + groupTopPadding + groupBottomPadding + 12));
            }); updateCanvasSize();
        }
        function layoutByDependency() {
            var levels = {}, cursors = {}, stageLayouts = {}, levelWidths = {}, levelOffsets = {}, currentX = 70; groupFrames = {};
            groups.forEach(function (group) { levels[group.groupId] = 0; });
            for (var pass = 0; pass < groups.length; pass++) {
                dependencies.forEach(function (item) {
                    var from = endpointGroup(item.predecessorType, item.predecessorId), to = endpointGroup(item.successorType, item.successorId);
                    if (from && to && from !== to) levels[to] = Math.min(groups.length, Math.max(levels[to] || 0, (levels[from] || 0) + 1));
                });
            }
            groups.forEach(function (group) { var groupNodeList = groupNodes(group.groupId), level = levels[group.groupId] || 0; stageLayouts[group.groupId] = buildGroupStageInfo(groupNodeList); levelWidths[level] = Math.max(levelWidths[level] || 0, stageLayouts[group.groupId].width); });
            for (var levelIndex = 0; levelIndex <= groups.length; levelIndex++) { if (levelWidths[levelIndex]) { levelOffsets[levelIndex] = currentX; currentX += levelWidths[levelIndex] + 110; } }
            groupStageInfo = stageLayouts;
            groups.forEach(function (group) {
                var level = levels[group.groupId] || 0, y = cursors[level] || 70, baseX = levelOffsets[level] || 70, layout = stageLayouts[group.groupId];
                positions[endpointKey('GROUP', group.groupId)] = {x: baseX, y: y};
                layout.stages.forEach(function (stage, stageIndex) { var currentY = y + 38; layout.buckets[stage].forEach(function (node) { positions[endpointKey('NODE', node.nodeId)] = {x: baseX + stageIndex * (nodeWidth + 54), y: currentY}; currentY += nodeHeightOf(node) + 14; }); });
                cursors[level] = y + (collapsedGroups[group.groupId] ? 82 : Math.max(125, layout.height + groupTopPadding + groupBottomPadding + 12));
            });
            updateCanvasSize();
        }
        function placeNewFlowElements() {
            var nextGroupY = 70;
            groups.forEach(function (group) {
                var groupPosition = positions[endpointKey('GROUP', group.groupId)];
                if (!groupPosition) return;
                var bottom = groupPosition.y + 96;
                groupNodes(group.groupId).forEach(function (node) {
                    var nodePosition = positions[endpointKey('NODE', node.nodeId)];
                    if (nodePosition) bottom = Math.max(bottom, nodePosition.y + nodeHeightOf(node) + 50);
                });
                nextGroupY = Math.max(nextGroupY, bottom + 36);
            });
            groups.forEach(function (group) {
                var groupKey = endpointKey('GROUP', group.groupId);
                if (!positions[groupKey]) {
                    positions[groupKey] = {x: 70, y: nextGroupY};
                    nextGroupY += 132;
                }
            });
            groups.forEach(function (group) {
                var groupNodeList = groupNodes(group.groupId);
                var existingNodes = groupNodeList.filter(function (node) { return !!positions[endpointKey('NODE', node.nodeId)]; });
                var newNodes = groupNodeList.filter(function (node) { return !positions[endpointKey('NODE', node.nodeId)]; });
                if (!newNodes.length) return;
                var baseX, baseY;
                if (existingNodes.length) {
                    baseX = Math.min.apply(Math, existingNodes.map(function (node) { return positions[endpointKey('NODE', node.nodeId)].x; }));
                    baseY = Math.max.apply(Math, existingNodes.map(function (node) { var position = positions[endpointKey('NODE', node.nodeId)]; return position.y + nodeHeightOf(node); })) + 14;
                } else {
                    var groupPosition = positions[endpointKey('GROUP', group.groupId)];
                    baseX = groupPosition.x;
                    baseY = groupPosition.y + 38;
                }
                newNodes.forEach(function (node, index) {
                    positions[endpointKey('NODE', node.nodeId)] = {x: baseX + (index % 3) * (nodeWidth + 14), y: baseY + Math.floor(index / 3) * (nodeHeightOf(node) + 14)};
                });
                expandGroupFrame(group.groupId);
            });
        }
        function endpointPoint(type, id, side) {
            if (type === 'NODE') { var node = nodeById[id]; if (node && collapsedGroups[node.groupId]) return endpointPoint('GROUP', node.groupId, side); var point = positions[endpointKey(type, id)] || {x: 100, y: 100}; return {x: point.x + (side === 'from' ? nodeWidth : 0), y: point.y + nodeHeightOf(node || {nodeId: id}) / 2}; }
            var group = groupById[id], index = groups.indexOf(group), bounds = groupBounds(group || {}, index < 0 ? 0 : index); return {x: bounds.x + (side === 'from' ? bounds.width : 0), y: bounds.y + 24};
        }
        function edgeGeometry(start, end, index) {
            if (end.x >= start.x) { var offset = Math.max(56, (end.x - start.x) * .42) + ((index % 5) - 2) * 10; return {path: 'M ' + start.x + ' ' + start.y + ' C ' + (start.x + offset) + ' ' + start.y + ', ' + (end.x - offset) + ' ' + end.y + ', ' + end.x + ' ' + end.y}; }
            var lane = Math.max(start.x, end.x) + 72 + (index % 8) * 18;
            return {path: 'M ' + start.x + ' ' + start.y + ' C ' + lane + ' ' + start.y + ', ' + lane + ' ' + end.y + ', ' + end.x + ' ' + end.y};
        }
        function renderCanvas() {
            if (dependencyDrag) clearDependencyDrag();
            updateCanvasSize(); while (svg.firstChild) svg.removeChild(svg.firstChild);
            var defs = makeSvg('defs', {});
            ['wait', 'dispatched', 'running', 'complete', 'exception', 'skipped', 'stopping', 'stopped', 'uninitialized'].forEach(function (name) { var marker = makeSvg('marker', {id: 'flowArrow-' + name, viewBox: '0 0 10 10', refX: 9, refY: 5, markerWidth: 9, markerHeight: 9, markerUnits: 'userSpaceOnUse', orient: 'auto'}); marker.appendChild(makeSvg('path', {d: 'M 0 0 L 10 5 L 0 10 z', fill: statusColor(name)})); defs.appendChild(marker); });
            var selectedMarker = makeSvg('marker', {id: 'flowArrow-selected', viewBox: '0 0 10 10', refX: 9, refY: 5, markerWidth: 9, markerHeight: 9, markerUnits: 'userSpaceOnUse', orient: 'auto'});
            selectedMarker.appendChild(makeSvg('path', {d: 'M 0 0 L 10 5 L 0 10 z', fill: '#7c3aed'}));
            defs.appendChild(selectedMarker);
            svg.appendChild(defs);
            groups.forEach(function (group, index) {
                var bounds = groupBounds(group, index), collapsed = !!collapsedGroups[group.groupId], groupNodeCount = groupNodes(group.groupId).length, stageInfo = groupStageInfo[group.groupId], dependencyInfo = groupDependencyInfo(group.groupId), isSelected = (selected && selected.kind === 'GROUP' && selected.id === group.groupId) || isSelectedEdgeEndpoint('GROUP', group.groupId), groupClass = ' status-' + statusClass(group.runStatus) + (dependencyInfo.hasExternalDependency ? ' has-external-dependency' : '') + (isSelected ? ' selected' : '');
                var groupBox = makeSvg('rect', {x: bounds.x, y: bounds.y, width: bounds.width, height: bounds.height, rx: 5, 'class': 'flow-group-box' + groupClass, 'data-flow-endpoint': 'true', 'data-flow-target': 'true', 'data-type': 'GROUP', 'data-id': group.groupId});
                groupBox.appendChild(makeSvg('title', {})).textContent = '拖动可整体移动组内节点';
                groupBox.addEventListener('click', function () { selectFlowObject('GROUP', group.groupId); });
                groupBox.addEventListener('pointerdown', function (event) { startGroupDrag(event, group.groupId, bounds); });
                svg.appendChild(groupBox);
                svg.appendChild(makeSvg('rect', {x: bounds.x, y: bounds.y, width: bounds.width, height: 29, rx: 5, 'class': 'flow-group-header' + groupClass, 'data-flow-group-id': group.groupId}));
                var collapseHit = makeSvg('rect', {x: bounds.x, y: bounds.y, width: Math.max(36, bounds.width - 18), height: 29, 'class': 'flow-group-collapse-hit', 'data-flow-endpoint': 'true', 'data-type': 'GROUP', 'data-id': group.groupId});
                collapseHit.appendChild(makeSvg('title', {})).textContent = collapsed ? '点击展开组' : '点击收起组';
                collapseHit.addEventListener('click', function (event) { event.preventDefault(); event.stopPropagation(); collapsedGroups[group.groupId] = !collapsedGroups[group.groupId]; renderCanvas(); });
                svg.appendChild(collapseHit);
                var collapse = makeSvg('text', {x: bounds.x + 10, y: bounds.y + 19, 'class': 'flow-group-collapse'}); collapse.textContent = collapsed ? '[+]' : '[-]';
                svg.appendChild(collapse);
                var label = makeSvg('text', {x: bounds.x + 38, y: bounds.y + 19, 'class': 'flow-group-label'}); label.textContent = group.groupName + ' | ' + groupNodeCount + ' 个节点'; svg.appendChild(label);
                var count = makeSvg('text', {x: bounds.x + bounds.width - 10, y: bounds.y + 19, 'text-anchor': 'end', 'class': 'flow-group-count', 'data-flow-group-count': group.groupId}); count.textContent = turnDateText(group.turnDate || workTurnDate) + ' | ' + statusText(group.runStatus); svg.appendChild(count);
                appendDependencyOutput(svg, 'GROUP', group.groupId, bounds.x + bounds.width - 2, bounds.y + 24);
                if (dependencyInfo.hasExternalDependency) { var dependencyCount = makeSvg('text', {x: bounds.x + 10, y: bounds.y + bounds.height - 10, 'class': 'flow-group-dependency-count'}); dependencyCount.textContent = '跨组依赖 ' + dependencyInfo.crossCount + ' 条'; svg.appendChild(dependencyCount); }
                if (!collapsed && stageInfo && stageInfo.stages.length > 1) stageInfo.stages.forEach(function (stage, stageIndex) { var stageX = bounds.x + 20 + stageIndex * (nodeWidth + 54), stageLabel = makeSvg('text', {x: stageX, y: bounds.y + 43, 'class': 'flow-stage-label'}); stageLabel.textContent = '阶段 ' + (stageIndex + 1); svg.appendChild(stageLabel); if (stageIndex) svg.appendChild(makeSvg('line', {x1: stageX - 27, y1: bounds.y + 34, x2: stageX - 27, y2: bounds.y + bounds.height - 8, 'class': 'flow-stage-divider'})); });
            });
            dependencies.forEach(function (item, edgeIndex) {
                if (!objectExists(item.predecessorType, item.predecessorId) || !objectExists(item.successorType, item.successorId)) return;
                var predecessorGroup = endpointGroup(item.predecessorType, item.predecessorId), successorGroup = endpointGroup(item.successorType, item.successorId), internal = predecessorGroup === successorGroup;
                if (internal && collapsedGroups[predecessorGroup]) return;
                var start = endpointPoint(item.predecessorType, item.predecessorId, 'from'), end = endpointPoint(item.successorType, item.successorId, 'to'), geometry = edgeGeometry(start, end, edgeIndex), highlighted = isHighlightedEdge(item), muted = selected && !highlighted, edgeStatus = dependencyStatusClass(item), pathClass = 'flow-edge status-' + edgeStatus + (internal ? ' flow-edge-internal' : ' flow-edge-external') + (muted ? ' flow-edge-muted' : '') + (highlighted ? ' selected' : '');
                if (!internal) svg.appendChild(makeSvg('path', {d: geometry.path, 'class': 'flow-edge-halo'}));
                var path = makeSvg('path', {d: geometry.path, 'marker-end': 'url(#flowArrow-' + (highlighted ? 'selected' : edgeStatus) + ')', 'class': pathClass});
                path.dataset.id = item.dependencyId;
                path.addEventListener('click', function (event) { event.stopPropagation(); selected = {kind: 'EDGE', id: item.dependencyId}; syncCanvasSearchSelection(); renderCanvas(); refreshSelectionUi(); });
                svg.appendChild(path);
            });
            nodes.forEach(function (node, index) {
                if (collapsedGroups[node.groupId]) return;
                var point = ensurePosition(endpointKey('NODE', node.nodeId), index), height = nodeHeightOf(node), lines = nodeNameLines(node), nodeStatusClass = statusClass(node.nodeRunStatus), nodeSelected = (selected && selected.kind === 'NODE' && selected.id === node.nodeId) || isSelectedEdgeEndpoint('NODE', node.nodeId) || isSelectedGroupNode(node), group = makeSvg('g', {transform: 'translate(' + point.x + ',' + point.y + ')', 'class': 'flow-node status-' + nodeStatusClass + (nodeSelected ? ' selected' : '')});
                group.dataset.flowEndpoint = 'true'; group.dataset.flowTarget = 'true'; group.dataset.type = 'NODE'; group.dataset.id = node.nodeId;
                group.appendChild(makeSvg('rect', {width: nodeWidth, height: height, rx: 4, 'class': 'node-body'}));
                group.appendChild(makeSvg('rect', {width: nodeWidth, height: 26, rx: 4, 'class': 'node-header'}));
                group.appendChild(makeSvg('rect', {width: 5, height: height, rx: 3, fill: nodeSelected ? '#7c3aed' : statusColor(nodeStatusClass), 'class': 'node-status-strip'}));
                var turnDate = makeSvg('text', {x: 12, y: 17, 'class': 'node-turn-date'}); turnDate.textContent = turnDateText(node.turnDate || workTurnDate); group.appendChild(turnDate);
                group.appendChild(makeSvg('rect', {x: nodeWidth - 76, y: 5, width: 64, height: 16, rx: 8, fill: nodeSelected ? '#7c3aed' : statusColor(nodeStatusClass), 'class': 'node-status-badge'}));
                var statusLabel = makeSvg('text', {x: nodeWidth - 44, y: 17, 'text-anchor': 'middle', 'class': 'node-status-label'}); statusLabel.textContent = statusText(node.nodeRunStatus); group.appendChild(statusLabel);
                var name = makeSvg('text', {x: 12, y: lines.length > 1 ? 44 : 51, 'class': 'node-name'}); lines.forEach(function (line, lineIndex) { var span = makeSvg('tspan', {x: 12, dy: lineIndex ? 15 : 0}); span.textContent = line; name.appendChild(span); }); group.appendChild(name);
                appendDependencyOutput(group, 'NODE', node.nodeId, nodeWidth - 2, height / 2);
                group.addEventListener('click', function () { selectFlowObject('NODE', node.nodeId); });
                group.addEventListener('mouseenter', function (event) { showNodeTooltip(node, event); });
                group.addEventListener('mousemove', positionNodeTooltip);
                group.addEventListener('mouseleave', hideNodeTooltip);
                group.addEventListener('pointerdown', function (event) { if (isDependencyPort(event.target)) return; var dragPoint = svgPoint(event), current = positions[endpointKey('NODE', node.nodeId)]; dragState = {kind: 'NODE', id: node.nodeId, dx: dragPoint.x - current.x, dy: dragPoint.y - current.y}; });
                svg.appendChild(group);
            });
            renderCanvasSelectionActions();
        }
        function scheduleCanvasRender() { if (renderFrame != null) return; renderFrame = window.requestAnimationFrame(function () { renderFrame = null; renderCanvas(); }); }
        function openGroupForm(group) {
            var editing = !!group, root = editing && group.groupType === 'ROOT', content = '<div class="flow-group-dialog" style="padding:18px 22px 8px;">'
                + '<label style="display:block;margin-bottom:12px;color:#4b5563;font-weight:normal;">组名称<font color="red">*</font><input class="form-control" name="groupName" maxlength="128" value="' + escapeHtml(group ? group.groupName : '') + '"></label>'
                + (root ? '' : '<label style="display:block;color:#4b5563;font-weight:normal;">组说明<input class="form-control" name="groupDesc" maxlength="500" value="' + escapeHtml(group ? group.groupDesc : '') + '"></label>')
                + '</div>';
            layer.open({
                type: 1,
                title: root ? '编辑默认组名称' : (editing ? '编辑组' : '新增组'),
                area: getModalArea(460, root ? 190 : 260),
                content: content,
                btn: [editing ? '保存修改' : '创建组', '取消'],
                success: function (layero) {
                    layero.find('[name=groupName]').focus();
                },
                yes: function (index, layero) {
                    var groupName = $.trim(layero.find('[name=groupName]').val());
                    if (!groupName) {
                        layer.msg('请输入组名称', {icon: 2});
                        return;
                    }
                    $.post('${request.contextPath}/flow/group/save', {
                        workId: workId,
                        groupId: editing ? group.groupId : '',
                        groupName: groupName,
                        groupDesc: root ? '' : layero.find('[name=groupDesc]').val()
                    }, function (response) {
                        if (response.code === 200) {
                            layer.close(index);
                            layer.msg(editing ? '组已保存' : '组已创建', {icon: 1});
                            reload();
                        } else {
                            layer.msg(response.msg || '保存失败', {icon: 2});
                        }
                    }, 'json').fail(function () {
                        layer.msg('保存失败', {icon: 2});
                    });
                }
            });
        }
        function editGroup(group) { openGroupForm(group); }
        function syncCollapsedGroups() {
            var active = {};
            groups.forEach(function (group) { active[group.groupId] = true; if (collapsedGroups[group.groupId] === undefined) collapsedGroups[group.groupId] = true; });
            Object.keys(collapsedGroups).forEach(function (groupId) { if (!active[groupId]) delete collapsedGroups[groupId]; });
            Object.keys(groupFrames).forEach(function (groupId) { if (!active[groupId]) delete groupFrames[groupId]; });
        }
        function renderCanvasSearchResults() {
            var input = $('#canvasObjectSearch'), target = $('#canvasObjectSearchResults'), keyword = input.val(), results = matchingEndpoints(keyword);
            if (!target.length) return;
            if (!$.trim(keyword)) { target.empty(); return; }
            target.html(results.items.length ? results.items.map(function (item) {
                return '<button type="button" class="flow-canvas-search-result" data-type="' + escapeHtml(item.type) + '" data-id="' + escapeHtml(item.id) + '"><i class="fa ' + (item.type === 'GROUP' ? 'fa-folder-o' : 'fa-cog') + '"></i><span>' + escapeHtml(canvasSearchLabel(item.type, item.id)) + '</span><small>' + (item.type === 'GROUP' ? '组' : '节点') + '</small></button>';
            }).join('') + searchResultHint(results) : '<div class="flow-batch-empty">未找到匹配的流程对象</div>');
        }
        function syncCanvasSearchSelection() {
            var input = $('#canvasObjectSearch'), target = $('#canvasObjectSearchResults');
            if (!input.length) return;
            if (selected && (selected.kind === 'GROUP' || selected.kind === 'NODE')) {
                input.val(canvasSearchLabel(selected.kind, selected.id));
            } else {
                input.val('');
            }
            target.empty();
        }
        function scrollCanvasToObject(type, id) {
            window.requestAnimationFrame(function () {
                var wrap = document.getElementById('canvasWrap'), target, group;
                if (type === 'GROUP') {
                    group = groupById[id];
                    if (!group) return;
                    target = groupBounds(group, groups.indexOf(group));
                } else {
                    var node = nodeById[id];
                    if (!node) return;
                    var point = ensurePosition(endpointKey('NODE', id), nodes.indexOf(node));
                    target = {x: point.x, y: point.y, width: nodeWidth, height: nodeHeightOf(node)};
                }
                wrap.scrollLeft = Math.max(0, (target.x + target.width / 2) * zoom - wrap.clientWidth / 2);
                wrap.scrollTop = Math.max(0, (target.y + target.height / 2) * zoom - wrap.clientHeight / 2);
            });
        }
        function selectFlowObject(type, id) {
            var groupId = type === 'GROUP' ? id : endpointGroup(type, id);
            if (type === 'NODE' && groupId && collapsedGroups[groupId]) {
                collapsedGroups[groupId] = false;
            }
            selected = {kind: type, id: id};
            syncCanvasSearchSelection();
            renderCanvas();
            refreshSelectionUi();
            scrollCanvasToObject(type, id);
        }
        function selectedGroup() { return selected && selected.kind === 'GROUP' ? groupById[selected.id] : null; }
        function renderCanvasSelectionActions() {
            var actions = $('#flowCanvasSelectionActions');
            if (!selected) { actions.empty().removeClass('is-edge-selection is-inline-selection is-inline-node is-inline-group').attr('hidden', true); return; }
            var editTitle, deleteTitle, deleteDisabled = false, showEdit = false, showRerun = false, showLog = false, edgePredecessor = '', edgeSuccessor = '';
            if (selected.kind === 'NODE') {
                var node = selectedNode();
                if (!node) { actions.empty().attr('hidden', true); return; }
                showRerun = true;
                showEdit = true;
                showLog = true;
                editTitle = '编辑节点';
                deleteTitle = '删除节点';
            } else if (selected.kind === 'GROUP') {
                var group = selectedGroup();
                if (!group) { actions.empty().attr('hidden', true); return; }
                showEdit = true;
                showLog = true;
                editTitle = group.groupType === 'ROOT' ? '编辑默认组名称' : '编辑组';
                deleteTitle = group.groupType === 'ROOT' ? '默认组不可删除' : '删除组';
                deleteDisabled = group.groupType === 'ROOT';
            } else if (selected.kind === 'EDGE') {
                var edge = dependencyById[selected.id];
                if (!edge) { actions.empty().attr('hidden', true); return; }
                deleteTitle = '删除依赖';
                edgePredecessor = endpointRelationLabel(edge.predecessorType, edge.predecessorId);
                edgeSuccessor = endpointRelationLabel(edge.successorType, edge.successorId);
            } else {
                actions.empty().attr('hidden', true);
                return;
            }
            var html = '';
            if (showLog) html += '<button type="button" class="btn btn-default btn-xs flow-canvas-log" title="查看运行日志"><i class="fa fa-file-text-o"></i></button>';
            if (showRerun) html += '<button type="button" class="btn btn-default btn-xs flow-canvas-rerun" title="重跑节点"><i class="fa fa-history"></i></button>';
            if (showEdit) html += '<button type="button" class="btn btn-default btn-xs flow-canvas-edit" title="' + editTitle + '"><i class="fa fa-edit"></i></button>';
            if (edgePredecessor) {
                html += '<div class="flow-canvas-edge-relation"><div class="flow-canvas-edge-node" title="' + escapeHtml(edgePredecessor) + '"><span class="flow-canvas-edge-role">前置</span><span class="flow-canvas-edge-name">' + escapeHtml(edgePredecessor) + '</span></div><div class="flow-canvas-edge-connector"><i class="fa fa-long-arrow-down"></i><button type="button" class="btn btn-danger btn-xs flow-canvas-delete" title="' + deleteTitle + '"><i class="fa fa-trash"></i></button></div><div class="flow-canvas-edge-node successor" title="' + escapeHtml(edgeSuccessor) + '"><span class="flow-canvas-edge-role">后置</span><span class="flow-canvas-edge-name">' + escapeHtml(edgeSuccessor) + '</span></div></div>';
            } else {
                html += '<button type="button" class="btn btn-danger btn-xs flow-canvas-delete" title="' + deleteTitle + '"' + (deleteDisabled ? ' disabled' : '') + '><i class="fa fa-trash"></i></button>';
            }
            actions.removeClass('is-edge-selection is-inline-selection is-inline-node is-inline-group is-below');
            if (edgePredecessor) actions.addClass('is-edge-selection');
            else if (selected.kind === 'NODE') actions.addClass('is-inline-selection is-inline-node');
            else actions.addClass('is-inline-selection is-inline-group');
            actions.html(html).removeAttr('hidden');
            positionCanvasSelectionActions();
        }
        function positionCanvasSelectionActions() {
            var actions = $('#flowCanvasSelectionActions');
            if (!selected || actions.is(':hidden')) return;
            var inline = actions.hasClass('is-inline-selection');
            if (inline && selected.kind === 'NODE') {
                var node = selectedNode(), nodePoint = node && positions[endpointKey('NODE', node.nodeId)];
                if (!nodePoint) return;
                var nodeScale = Math.max(.72, zoom), nodeWidthPx = actions.outerWidth() * nodeScale, nodeHeightPx = actions.outerHeight() * nodeScale;
                actions.removeClass('is-below').css({
                    left: (nodePoint.x * zoom + (nodeWidth - 76) * zoom - nodeWidthPx - 5 * zoom) + 'px',
                    top: (nodePoint.y * zoom + Math.max(0, (26 * zoom - nodeHeightPx) / 2)) + 'px',
                    transform: 'scale(' + nodeScale + ')'
                });
                return;
            }
            if (inline && selected.kind === 'GROUP') {
                var group = selectedGroup();
                if (!group) return;
                var groupIndex = groups.indexOf(group), bounds = groupBounds(group, groupIndex < 0 ? 0 : groupIndex);
                var count = svg.querySelector('[data-flow-group-count="' + group.groupId + '"]');
                var countWidth = count && count.getComputedTextLength ? Math.max(86, count.getComputedTextLength()) : 100;
                var groupScale = Math.max(.72, zoom), groupWidthPx = actions.outerWidth() * groupScale, groupHeightPx = actions.outerHeight() * groupScale;
                actions.removeClass('is-below').css({
                    left: (bounds.x * zoom + (bounds.width - 10 - countWidth) * zoom - groupWidthPx - 6 * zoom) + 'px',
                    top: (bounds.y * zoom + Math.max(0, (29 * zoom - groupHeightPx) / 2)) + 'px',
                    transform: 'scale(' + groupScale + ')'
                });
                return;
            }
            var anchor = null;
            if (selected.kind === 'NODE') {
                var node = selectedNode(), nodePoint = node && positions[endpointKey('NODE', node.nodeId)];
                if (nodePoint) anchor = {x: nodePoint.x + nodeWidth / 2, y: nodePoint.y};
            } else if (selected.kind === 'GROUP') {
                var group = selectedGroup();
                if (group) { var groupIndex = groups.indexOf(group), bounds = groupBounds(group, groupIndex < 0 ? 0 : groupIndex); anchor = {x: bounds.x + bounds.width / 2, y: bounds.y}; }
            } else if (selected.kind === 'EDGE') {
                var edgePath = Array.prototype.filter.call(svg.querySelectorAll('path.flow-edge'), function (path) {
                    return String(path.dataset.id) === String(selected.id);
                })[0];
                if (edgePath && edgePath.getTotalLength()) {
                    var midpoint = edgePath.getPointAtLength(edgePath.getTotalLength() / 2);
                    anchor = {x: midpoint.x, y: midpoint.y};
                } else {
                    var edge = dependencyById[selected.id];
                    if (edge) { var start = endpointPoint(edge.predecessorType, edge.predecessorId, 'from'), end = endpointPoint(edge.successorType, edge.successorId, 'to'); anchor = {x: (start.x + end.x) / 2, y: (start.y + end.y) / 2}; }
                }
            }
            if (!anchor) return;
            var width = actions.outerWidth(), height = actions.outerHeight(), scaledX = anchor.x * zoom, scaledY = anchor.y * zoom;
            var left = Math.max(8, Math.min(scaledX - width / 2, canvasSize.width * zoom - width - 8));
            var top = scaledY - height - 10, isBelow = top < 8;
            if (isBelow) top = scaledY + 10;
            top = Math.max(8, Math.min(top, canvasSize.height * zoom - height - 8));
            actions.toggleClass('is-below', isBelow).css({left: left + 'px', top: top + 'px', transform: ''});
        }
        function refreshSelectionUi() { updateNodeActionMenu(); }
        function resolveGroupRunStatus(groupId) {
            var flags = {}, groupNodeList = groupNodes(groupId);
            if (!groupNodeList.length) return 0;
            groupNodeList.forEach(function (node) { flags[node.nodeRunStatus == null ? 0 : node.nodeRunStatus] = true; });
            if (flags[3]) return 3;
            if (flags[7]) return 7;
            if (flags[6]) return 6;
            if (flags[1]) return 1;
            if (flags[4]) return 4;
            if (flags[0]) return 0;
            if (flags[8]) return 8;
            if (flags[2]) return 2;
            return flags[5] ? 5 : 2;
        }
        function refreshGroupRunStatuses() {
            groups.forEach(function (group) { group.runStatus = resolveGroupRunStatus(group.groupId); });
        }
        function replaceStatusClass(element, status) {
            if (!element) return;
            var classes = String(element.getAttribute('class') || '').split(/\s+/).filter(function (name) { return name && name.indexOf('status-') !== 0; });
            classes.push('status-' + statusClass(status));
            element.setAttribute('class', classes.join(' '));
        }
        function findFlowNode(nodeId) {
            return Array.prototype.filter.call(svg.querySelectorAll('.flow-node'), function (element) {
                return String(element.dataset.id) === String(nodeId);
            })[0];
        }
        function updateNodeRuntimeVisual(node) {
            var element = findFlowNode(node.nodeId);
            if (!element) return;
            var selectedNode = selected && selected.kind === 'NODE' && selected.id === node.nodeId;
            var status = statusClass(node.nodeRunStatus), color = selectedNode ? '#7c3aed' : statusColor(status);
            replaceStatusClass(element, node.nodeRunStatus);
            var strip = element.querySelector('.node-status-strip');
            var badge = element.querySelector('.node-status-badge');
            var label = element.querySelector('.node-status-label');
            var date = element.querySelector('.node-turn-date');
            if (strip) strip.setAttribute('fill', color);
            if (badge) badge.setAttribute('fill', color);
            if (label) label.textContent = statusText(node.nodeRunStatus);
            if (date) date.textContent = turnDateText(node.turnDate || workTurnDate);
            if (String(hoveredNodeId) === String(node.nodeId)) nodeTooltip.innerHTML = nodeTooltipContent(node);
        }
        function updateGroupRuntimeVisual(group) {
            Array.prototype.forEach.call(svg.querySelectorAll('.flow-group-box'), function (element) {
                if (String(element.dataset.id) === String(group.groupId)) replaceStatusClass(element, group.runStatus);
            });
            Array.prototype.forEach.call(svg.querySelectorAll('[data-flow-group-id]'), function (element) {
                if (String(element.getAttribute('data-flow-group-id')) === String(group.groupId)) replaceStatusClass(element, group.runStatus);
            });
            Array.prototype.forEach.call(svg.querySelectorAll('[data-flow-group-count]'), function (element) {
                if (String(element.getAttribute('data-flow-group-count')) === String(group.groupId)) {
                    element.textContent = turnDateText(group.turnDate || workTurnDate) + ' | ' + statusText(group.runStatus);
                }
            });
        }
        function updateDependencyRuntimeVisual(changedNodeIds, changedGroupIds) {
            var changedDependencyIds = {};
            function collectDependencies(type, changedIds) {
                Object.keys(changedIds).forEach(function (id) {
                    (dependenciesByEndpoint[endpointKey(type, id)] || []).forEach(function (dependency) { changedDependencyIds[dependency.dependencyId] = dependency; });
                });
            }
            collectDependencies('NODE', changedNodeIds);
            collectDependencies('GROUP', changedGroupIds);
            Array.prototype.forEach.call(svg.querySelectorAll('path.flow-edge'), function (element) {
                var dependency = changedDependencyIds[element.dataset.id];
                if (!dependency) return;
                var edgeStatus = dependencyStatusClass(dependency);
                replaceStatusClass(element, edgeStatus);
                element.setAttribute('marker-end', 'url(#flowArrow-' + (element.classList.contains('selected') ? 'selected' : edgeStatus) + ')');
            });
        }
        function renderRuntimeVisuals(changedNodeIds, changedGroupIds) {
            nodes.forEach(function (node) { if (changedNodeIds[node.nodeId]) updateNodeRuntimeVisual(node); });
            groups.forEach(function (group) { if (changedGroupIds[group.groupId]) updateGroupRuntimeVisual(group); });
            updateDependencyRuntimeVisual(changedNodeIds, changedGroupIds);
        }
        function applyRuntimeUpdate(payload) {
            if (!payload || String(payload.workId || '') !== String(workId)) return;
            if (dragState || dependencyDrag) {
                pendingRuntimeUpdate = payload;
                return;
            }
            var work = payload.work || {};
            var workTurnDateChanged = false;
            if (Object.prototype.hasOwnProperty.call(work, 'runStatus')) workRuntime.runWorkStatus = work.runStatus;
            if (Object.prototype.hasOwnProperty.call(work, 'turnDate')) {
                workTurnDateChanged = workTurnDate !== (work.turnDate || '');
                workRuntime.workTurnDate = work.turnDate;
                workTurnDate = work.turnDate || '';
            }
            var changedNodeIds = {}, previousGroupStatuses = {};
            groups.forEach(function (group) { previousGroupStatuses[group.groupId] = group.runStatus; });
            (payload.nodes || []).forEach(function (update) {
                var node = nodeById[update.nodeId];
                if (!node) return;
                changedNodeIds[node.nodeId] = true;
                if (Object.prototype.hasOwnProperty.call(update, 'runStatus')) node.nodeRunStatus = update.runStatus;
                if (Object.prototype.hasOwnProperty.call(update, 'turnDate')) node.turnDate = update.turnDate;
                if (Object.prototype.hasOwnProperty.call(update, 'retryTimes')) node.currentRetryTimes = update.retryTimes;
            });
            refreshGroupRunStatuses();
            var changedGroupIds = {};
            groups.forEach(function (group) {
                if (workTurnDateChanged || previousGroupStatuses[group.groupId] !== group.runStatus) changedGroupIds[group.groupId] = true;
            });
            renderRunControls();
            renderRuntimeVisuals(changedNodeIds, changedGroupIds);
            refreshSelectionUi();
        }
        function connectRuntimeStream() {
            if (!window.EventSource || runtimeEventSource || !workId) return;
            runtimeEventSource = new EventSource('${request.contextPath}/flow/runtime/stream?workId=' + encodeURIComponent(workId));
            runtimeEventSource.addEventListener('runtime-update', function (event) {
                try {
                    applyRuntimeUpdate(JSON.parse(event.data));
                } catch (error) {
                    // Ignore malformed events; EventSource reconnects automatically when needed.
                }
            });
        }
        function limitedSearchResults(matches) { return {items: matches.slice(0, maxDependencySearchResults), truncated: matches.length > maxDependencySearchResults}; }
        function searchResultHint(results) { return results.truncated ? '<div class="flow-batch-empty">匹配结果较多，仅显示前 ' + maxDependencySearchResults + ' 项，请继续输入缩小范围</div>' : ''; }
        function matchingEndpoints(keyword) { var value = $.trim(String(keyword || '')).toLowerCase(), matches = []; if (!value) return limitedSearchResults(matches); groups.forEach(function (group) { if ((String(group.groupName || '') + ' ' + String(group.groupId || '')).toLowerCase().indexOf(value) >= 0) matches.push({type: 'GROUP', id: group.groupId}); }); nodes.forEach(function (node) { if ((String(node.nodeName || '') + ' ' + String(node.nodeId || '')).toLowerCase().indexOf(value) >= 0) matches.push({type: 'NODE', id: node.nodeId}); }); return limitedSearchResults(matches); }
        function renderRunControls() {
            var runStatus = workRuntime.runWorkStatus;
            var hasRunSnapshot = runStatus !== null && runStatus !== undefined;
            var scheduleReady = !!workRuntime.scheduleConfigured;
            var scheduleRunning = workRuntime.scheduleStatus === 1 || workRuntime.scheduleStatus === '1';
            var isActive = runStatus === 1 || runStatus === 4;
            var isTurnWork = workRuntime.workType === 0 || workRuntime.workType === '0';
            $('#flowStartSchedule').toggle(scheduleReady && !scheduleRunning);
            $('#flowStopSchedule').toggle(scheduleReady && scheduleRunning);
            $('#flowRerun').toggle((hasRunSnapshot && !isActive) || (!hasRunSnapshot && isTurnWork));
            $('#flowStopRun').toggle(runStatus === 0 || runStatus === 1 || runStatus === 4 || runStatus === 7);
            $('#flowRecover').toggle(runStatus === 3 || runStatus === 6 || runStatus === 7);
            $('#flowWorkName').text(workRuntime.workName || '-');
            $('#flowWorkId').text(workId || '-');
            $('#flowWorkType').text(workRuntime.workTypeName || '-');
            $('#flowWorkTurnDate').text(workTurnDate || '-');
            $('#flowRunStatus').removeClass('status-wait status-dispatched status-running status-complete status-exception status-skipped status-stopping status-stopped status-uninitialized').addClass('status-' + statusClass(hasRunSnapshot ? runStatus : 8)).text(hasRunSnapshot ? statusText(runStatus) : '未初始化');
        }
        function flowRequest(path) { return $.post('${request.contextPath}' + path, {workId: workId}, null, 'json'); }
        function isSuccessResponse(response) { return response && response.code === 200; }
        function applyFlowData(groupResponse, elementResponse, dependencyResponse) {
            var content = elementResponse.content || {}, groupStatusMap = {};
            (groupResponse.content || []).forEach(function (group) { groupStatusMap[group.groupId] = group.runStatus; });
            workRuntime = content;
            workTurnDate = content.workTurnDate || '';
            groups = (content.groups || groupResponse.content || []).map(function (group) {
                if (Object.prototype.hasOwnProperty.call(groupStatusMap, group.groupId)) group.runStatus = groupStatusMap[group.groupId];
                return group;
            });
            nodes = content.nodes || [];
            dependencies = dependencyResponse.content || [];
            rebuildElementIndexes();
            rebuildDependencyIndexes();
            syncCollapsedGroups();
            if (needsDependencyLayout) {
                positions = {};
                layoutByDependency();
                needsDependencyLayout = false;
            } else if (!Object.keys(positions).length) {
                layoutDefaults();
            } else {
                placeNewFlowElements();
            }
            renderRunControls();
            renderCanvas();
            refreshSelectionUi();
        }
        function loadDependencies() {
            var requestVersion = ++flowLoadVersion;
            return flowRequest('/flow/dependencies').done(function (response) {
                if (requestVersion !== flowLoadVersion) return;
                if (!isSuccessResponse(response)) { layer.msg('加载依赖关系失败，请刷新后重试', {icon: 2}); return; }
                dependencies = response.content || [];
                rebuildDependencyIndexes();
                if (needsDependencyLayout) {
                    positions = {};
                    layoutByDependency();
                    needsDependencyLayout = false;
                }
                renderCanvas();
            }).fail(function () {
                if (requestVersion === flowLoadVersion) layer.msg('加载依赖关系失败，请刷新后重试', {icon: 2});
            });
        }
        function reload() {
            var requestVersion = ++flowLoadVersion;
            return $.when(flowRequest('/flow/groups'), flowRequest('/flow/elements'), flowRequest('/flow/dependencies')).done(function (groupResult, elementResult, dependencyResult) {
                if (requestVersion !== flowLoadVersion) return;
                var groupResponse = groupResult[0], elementResponse = elementResult[0], dependencyResponse = dependencyResult[0];
                if (!isSuccessResponse(groupResponse) || !isSuccessResponse(elementResponse) || !isSuccessResponse(dependencyResponse)) {
                    layer.msg('加载流程数据失败，请刷新后重试', {icon: 2});
                    return;
                }
                applyFlowData(groupResponse, elementResponse, dependencyResponse);
            }).fail(function () {
                if (requestVersion === flowLoadVersion) layer.msg('加载流程数据失败，请检查网络后重试', {icon: 2});
            });
        }
        function refreshAndLayout() { needsDependencyLayout = true; reload(); }
        function confirmDelete(title, message, callback) { layer.confirm(message, {icon: 3, title: title, btn: ['确认删除', '取消']}, function (index) { layer.close(index); callback(); }); }
        function deleteDependency(id) { var edge = dependencyById[id], message = edge ? '确认删除依赖？<div style="margin-top:8px;color:#64748b;font-size:12px;line-height:18px;">' + escapeHtml(endpointLabel(edge.predecessorType, edge.predecessorId) + ' -> ' + endpointLabel(edge.successorType, edge.successorId)) + '</div>' : '确认删除该依赖？'; confirmDelete('删除依赖', message, function () { $.post('${request.contextPath}/flow/dependency/delete', {workId: workId, dependencyId: id}, function (response) { if (response.code === 200) { selected = null; syncCanvasSearchSelection(); loadDependencies(); refreshSelectionUi(); } else layer.msg(response.msg || '删除失败', {icon: 2}); }, 'json').fail(function () { layer.msg('删除依赖失败，请检查网络后重试', {icon: 2}); }); }); }
        // 使用表单提交下载，浏览器可直接处理包含组、各节点类型和依赖关系 Sheet 的工作簿。
        function downloadDependencyFile(url) { var form = $('<form method="post" style="display:none"></form>').attr('action', url).append($('<input type="hidden" name="workId">').val(workId)); $('body').append(form); form.trigger('submit'); window.setTimeout(function () { form.remove(); }, 0); }
        // multipart 请求保留工作簿原始文件流；成功后重新加载组、节点和依赖关系。
        function importDependencyFile(file) { var formData = new FormData(); formData.append('workId', workId); formData.append('file', file); $.ajax({url: '${request.contextPath}/flow/dependency/import', type: 'POST', data: formData, processData: false, contentType: false, dataType: 'json', success: function (response) { if (response.code === 200) { layer.msg('已导入 ' + response.content + ' 条流程配置', {icon: 1}); needsDependencyLayout = true; reload(); } else layer.msg(response.msg || '导入流程配置失败', {icon: 2}); }, error: function () { layer.msg('导入流程配置失败，请检查工作簿和 Sheet 后重试', {icon: 2}); }}); }
        function saveDependency(predecessor, successor) { if (!predecessor || !successor || predecessor.type === successor.type && predecessor.id === successor.id) { layer.msg('依赖对象不能为空且不能依赖自身', {icon: 2}); return; } $.post('${request.contextPath}/flow/dependency/save', {workId: workId, predecessorType: predecessor.type, predecessorId: predecessor.id, successorType: successor.type, successorId: successor.id}, function (response) { if (response.code === 200) loadDependencies(); else layer.msg(response.msg || '保存依赖失败', {icon: 2}); }, 'json').fail(function () { layer.msg('保存依赖失败，请检查网络后重试', {icon: 2}); }); }
        function runWorkAction(path, successText) {
            $.post('${request.contextPath}/work/' + path, {workId: workId}, function (response) {
                if (response.code === 200) {
                    layer.msg(response.msg || successText, {icon: 1});
                    reload();
                } else {
                    layer.msg(response.msg || '操作失败', {icon: 2});
                }
            }, 'json').fail(function () { layer.msg('操作失败', {icon: 2}); });
        }
        function localDateText() {
            var now = new Date();
            var month = String(now.getMonth() + 1).padStart(2, '0');
            var day = String(now.getDate()).padStart(2, '0');
            return now.getFullYear() + '-' + month + '-' + day;
        }
        function openFlowRerun() {
            var isTurn = workRuntime.workType === 0 || workRuntime.workType === '0';
            var today = localDateText();
            var defaultDate = workTurnDate || today;
            var dateInput = isTurn
                ? '<div style="margin-bottom:8px;color:#4b5563;">重跑翻牌日期</div><input id="rerunWorkTurnDate" type="date" class="form-control" value="' + escapeHtml(defaultDate) + '" max="' + escapeHtml(today) + '">'
                : '';
            var message = isTurn
                ? '请选择本次重跑的起始翻牌日期。首次重跑默认使用今天。'
                : '普通作业将完整重跑当前批次。';
            layer.open({
                type: 1,
                title: '重跑流程',
                area: ['460px', '290px'],
                content: '<div style="padding:18px 22px 10px;">' + dateInput + '<label style="display:block;margin-top:12px;color:#b45309;font-weight:normal;"><input id="forceRerunWork" type="checkbox"> 强制重跑</label><p style="margin:6px 0 0;color:#8a96a3;font-size:12px;line-height:18px;">' + message + ' 勾选强制重跑后会跳过客户端旧任务退出确认，旧任务未退出时可能并发执行。</p></div>',
                btn: ['确认重跑', '取消'],
                yes: function (index) {
                    var turnDate = isTurn ? $('#rerunWorkTurnDate').val() : '';
                    var force = $('#forceRerunWork').prop('checked');
                    if (isTurn && !/^\d{4}-\d{2}-\d{2}$/.test(turnDate)) {
                        layer.msg('请选择正确的翻牌日期', {icon: 2});
                        return;
                    }
                    $.post('${request.contextPath}/work/rerunRunWork', {workId: workId, turnDate: turnDate, force: force}, function (response) {
                        if (response.code === 200) {
                            layer.close(index);
                            layer.msg(force ? '已强制重跑' : '重跑设置成功', {icon: 1});
                            reload();
                        } else {
                            layer.msg(response.msg || '重跑设置失败', {icon: 2});
                        }
                    }, 'json').fail(function () { layer.msg('重跑设置失败', {icon: 2}); });
                }
            });
        }
        function openNodeForm(url, title) { layer.open({type: 2, area: getModalArea(1160, 780), title: title, maxmin: true, content: url, btn: ['保存', '取消'], yes: function (index, layero) { var win = layero.find('iframe')[0].contentWindow, form = win.document.querySelector('form'); if (win.beforeSubmit && !win.beforeSubmit()) return; $.ajax({url: form.getAttribute('action'), type: 'POST', data: new FormData(form), processData: false, contentType: false, success: function (response) { if (response.code === 200) { layer.close(index); reload(); } else layer.msg(response.msg || '保存失败', {icon: 2}); }}); }}); }
        function openNodeLog(url, title) { layer.open({type: 2, area: getModalArea(1180, 720), title: title, closeBtn: 1, shade: .6, shadeClose: true, maxmin: true, anim: 0, content: url}); }
        function selectedNode() { return selected && selected.kind === 'NODE' ? nodeById[selected.id] : null; }
        function updateNodeActionMenu() {
            var node = selectedNode(), logTitle = '查看当前作业的所有运行日志';
            if (selected && selected.kind === 'GROUP') logTitle = '查看组【' + (groupMap[selected.id] || selected.id) + '】的运行日志';
            if (node) logTitle = '查看节点【' + node.nodeName + '】的运行日志';
            $('#viewFlowLog').attr('title', logTitle);
        }
        function openFlowLog() {
            var params = {workId: workId}, title = '作业运行日志', node = selectedNode();
            if (selected && selected.kind === 'GROUP') { params.groupId = selected.id; title = '组运行日志【' + (groupMap[selected.id] || selected.id) + '】'; }
            if (node) { params.nodeId = node.nodeId; title = '节点运行日志【' + node.nodeName + '】'; }
            openNodeLog('${request.contextPath}/node/viewLogModel?' + $.param(params), title);
        }
        function deleteSelectedNode(node) { confirmDelete('删除节点', '确认删除节点【' + escapeHtml(node.nodeName) + '】？相关依赖和文件配置会一并删除。', function () { $.post('${request.contextPath}/node/delete', {id: node.nodeId}, function (response) { if (response.code === 200) { selected = null; reload(); } else layer.msg(response.msg || '删除失败', {icon: 2}); }, 'json').fail(function () { layer.msg('删除节点失败，请检查网络后重试', {icon: 2}); }); }); }
        function deleteSelectedGroup(group) { var nodeCount = groupNodes(group.groupId).length, dependencyInfo = groupDependencyInfo(group.groupId), message = '确认删除组【' + escapeHtml(group.groupName) + '】？'; if (nodeCount || dependencyInfo.relatedCount) message += '<div style="margin-top:8px;color:#b91c1c;font-size:12px;line-height:18px;">将同时删除 ' + nodeCount + ' 个内部节点及 ' + dependencyInfo.relatedCount + ' 条关联依赖，节点文件配置也会删除。</div>'; confirmDelete('删除组', message, function () { $.post('${request.contextPath}/flow/group/delete', {workId: workId, groupId: group.groupId}, function (response) { if (response.code === 200) { selected = null; reload(); } else layer.msg(response.msg || '删除失败', {icon: 2}); }, 'json').fail(function () { layer.msg('删除组失败，请检查网络后重试', {icon: 2}); }); }); }
        function openNodeRerun(node) {
            if (node.nodeRunStatus === 1 || node.nodeRunStatus === 4) { layer.msg('节点处于进行中或已下发状态，不能重跑', {icon: 2}); return; }
            var hasTurnDate = !!(workTurnDate || node.turnDate), defaultTurnDate = workTurnDate || node.turnDate || '', maxTurnDate = workTurnDate || '';
            var dateInput = hasTurnDate ? '<div style="margin-bottom:8px;color:#4b5563;">重跑日期</div><input id="rerunTurnDate" type="date" class="form-control" value="' + escapeHtml(defaultTurnDate) + '" max="' + escapeHtml(maxTurnDate) + '">' : '';
            var dependentOption = '<label style="display:block;margin-top:12px;color:#4b5563;font-weight:normal;"><input id="rerunDependentNodes" type="checkbox"> 依赖节点重跑</label>';
            var content = '<div style="padding:18px 22px 10px;">' + dateInput + dependentOption + '<p style="margin:10px 0 0;color:#8a96a3;font-size:12px;line-height:18px;">勾选后会将所选节点及其后续依赖节点回拨至所选日期；翻牌作业将从该日逐日执行，直至追平当前批次。</p></div>';
            layer.open({type: 1, title: '节点重跑【' + escapeHtml(node.nodeName) + '】', area: ['460px', '290px'], content: content, btn: ['确认重跑', '取消'], yes: function (index) {
                var rerunDependents = $('#rerunDependentNodes').prop('checked');
                var turnDate = hasTurnDate ? $('#rerunTurnDate').val() : '';
                if (hasTurnDate && !/^\d{4}-\d{2}-\d{2}$/.test(turnDate)) { layer.msg('请选择正确的重跑日期', {icon: 2}); return; }
                var url = rerunDependents ? '${request.contextPath}/node/rerunDependencies' : '${request.contextPath}/node/rerun';
                var data = {nodeId: node.nodeId, turnDate: turnDate};
                $.post(url, data, function (response) {
                    if (response.code === 200) { layer.close(index); layer.msg(rerunDependents ? '所选节点及其依赖节点已回拨，等待调度追平' : '所选节点已重跑，等待调度执行', {icon: 1}); reload(); } else { layer.msg(response.msg || '节点重跑失败', {icon: 2}); }
                }, 'json').fail(function () { layer.msg('节点重跑失败', {icon: 2}); });
            }});
        }
        $(document).on('click', '#downloadDependencyTemplate', function () { downloadDependencyFile('${request.contextPath}/flow/dependency/template'); });
        $(document).on('click', '#exportDependencies', function () { downloadDependencyFile('${request.contextPath}/flow/dependency/export'); });
        $(document).on('click', '#importDependencies', function () { $('#dependencyImportFile').val('').trigger('click'); });
        $('#dependencyImportFile').on('change', function () { if (this.files && this.files[0]) importDependencyFile(this.files[0]); });
        $('#flowStartSchedule').on('click', function () { runWorkAction('startSchedule', '定时已启动'); });
        $('#flowStopSchedule').on('click', function () { runWorkAction('stopSchedule', '定时已停止'); });
        $('#flowRerun').on('click', openFlowRerun);
        $('#flowStopRun').on('click', function () {
            var stopping = workRuntime.runWorkStatus === 7;
            var message = stopping
                ? '当前批次正在停止中，将重新确认客户端任务是否已退出。'
                : '确认停止当前批次？已下发或运行中的节点将进入停止中，等待客户端确认退出。';
            layer.confirm(message, {icon: 3, title: stopping ? '确认停止状态' : '停止当前批次'}, function (index) {
                layer.close(index);
                runWorkAction('stopLatestRunWork', stopping ? '停止状态确认已发送' : '停止请求已发送');
            });
        });
        $('#flowRecover').on('click', function () {
            var content = '<div style="padding:18px 22px 8px;"><p style="margin:0;color:#4b5563;line-height:20px;">恢复会继续执行异常或已停止的节点，已完成和已跳过节点不会重跑。</p><label style="display:block;margin-top:14px;color:#b45309;font-weight:normal;"><input id="forceRecoverRun" type="checkbox"> 强制恢复重跑</label><p style="margin:6px 0 0;color:#8a96a3;font-size:12px;line-height:18px;">未勾选时必须确认客户端旧任务已退出。勾选后会跳过确认，旧任务未退出时可能并发执行。</p></div>';
            layer.open({type: 1, title: '恢复作业', area: ['480px', '240px'], content: content, btn: ['确认恢复', '取消'], yes: function (index) {
                var force = $('#forceRecoverRun').prop('checked');
                $.post('${request.contextPath}/work/recoverRunWork', {workId: workId, force: force}, function (response) {
                    if (response.code === 200) { layer.close(index); layer.msg(force ? '已强制恢复重跑' : '恢复成功', {icon: 1}); reload(); }
                    else { layer.msg(response.msg || '恢复失败', {icon: 2}); }
                }, 'json').fail(function () { layer.msg('恢复失败', {icon: 2}); });
            }});
        });
        $('#viewFlowLog').on('click', openFlowLog);
        $('#addNodeAction').on('click', function () { openNodeForm('${request.contextPath}/node/addModel?workId=' + encodeURIComponent(workId) + '&fromFlow=true', '新增节点'); });
        $('#addGroupAction').on('click', function () { openGroupForm(null); });
        $('#flowCanvasSelectionActions').on('click', '.flow-canvas-delete', function (event) {
            event.preventDefault();
            event.stopPropagation();
            if (this.disabled || !selected) return;
            if (selected.kind === 'NODE') { var node = selectedNode(); if (node) deleteSelectedNode(node); return; }
            if (selected.kind === 'GROUP') { var group = selectedGroup(); if (group && group.groupType !== 'ROOT') deleteSelectedGroup(group); return; }
            if (selected.kind === 'EDGE') deleteDependency(selected.id);
        });
        $('#flowCanvasSelectionActions').on('click', '.flow-canvas-edit', function (event) {
            event.preventDefault();
            event.stopPropagation();
            if (!selected) return;
            if (selected.kind === 'NODE') {
                var node = selectedNode();
                if (node) openNodeForm('${request.contextPath}/node/updateModel?workNodeId=' + encodeURIComponent(node.nodeId) + '&fromFlow=true', '编辑节点');
            } else if (selected.kind === 'GROUP') {
                var group = selectedGroup();
                if (group) editGroup(group);
            }
        });
        $('#flowCanvasSelectionActions').on('click', '.flow-canvas-rerun', function (event) {
            event.preventDefault();
            event.stopPropagation();
            var node = selectedNode();
            if (node) openNodeRerun(node);
        });
        $('#flowCanvasSelectionActions').on('click', '.flow-canvas-log', function (event) {
            event.preventDefault();
            event.stopPropagation();
            openFlowLog();
        });
        $('#refreshAndLayout').on('click', refreshAndLayout);
        $('#canvasObjectSearch').on('input', renderCanvasSearchResults);
        $(document).on('click', '.flow-canvas-search-result', function () { selectFlowObject($(this).data('type'), $(this).data('id')); });
        $('#zoomIn').on('click', function () { indicateCanvasTool(this); zoom = Math.min(1.8, +(zoom + .1).toFixed(2)); updateCanvasSize(); });
        $('#zoomOut').on('click', function () { indicateCanvasTool(this); zoom = Math.max(.5, +(zoom - .1).toFixed(2)); updateCanvasSize(); });
        $('#fitCanvas').on('click', function () { indicateCanvasTool(this); var wrap = document.getElementById('canvasWrap'), fit = Math.min((wrap.clientWidth - 20) / canvasSize.width, (wrap.clientHeight - 20) / canvasSize.height); zoom = Math.max(.5, Math.min(1.2, +fit.toFixed(2))); updateCanvasSize(); wrap.scrollLeft = 0; wrap.scrollTop = 0; });
        svg.addEventListener('click', function (event) { if (event.target === svg) { selected = null; syncCanvasSearchSelection(); renderCanvas(); refreshSelectionUi(); } });
        document.addEventListener('pointermove', function (event) { if (dragState) { var point = svgPoint(event); if (dragState.kind === 'GROUP') updateGroupDrag(point); else updateNodeDrag(point); scheduleCanvasRender(); } updateDependencyDrag(event); });
        document.addEventListener('pointerup', function (event) { if (dragState) dragState = null; if (dependencyDrag) { var source = {type: dependencyDrag.type, id: dependencyDrag.id}, target = dependencyTarget(event); if (target && !isSameDependencyEndpoint(target)) { clearDependencyDrag(); saveDependency(source, {type: target.dataset.type, id: target.dataset.id}); } else if (!dependencyDrag.moved && !dependencyDrag.locked) { dependencyDrag.locked = true; stopDependencyAutoScroll(); } else { clearDependencyDrag(); } } if (pendingRuntimeUpdate) { var runtimeUpdate = pendingRuntimeUpdate; pendingRuntimeUpdate = null; applyRuntimeUpdate(runtimeUpdate); } });
        document.addEventListener('pointercancel', clearDependencyDrag);
        document.addEventListener('keydown', function (event) { if (event.key === 'Escape' && dependencyDrag) { event.preventDefault(); clearDependencyDrag(); } });
        window.addEventListener('blur', clearDependencyDrag);
        $(window).on('beforeunload', function () { if (runtimeEventSource) runtimeEventSource.close(); });
        reload().then(connectRuntimeStream);
    }());
</script>
</body>
</html>
