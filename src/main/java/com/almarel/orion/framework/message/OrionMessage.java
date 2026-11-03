package com.almarel.orion.framework.message;

/**
 * Центральный каталог всех сообщений плагина.
 * Связывает ключи из chat.yml с их семантическим типом.
 */
public enum OrionMessage {

    // ==========================================
    // 1. GLOBAL & COMMANDS
    // ==========================================
    CMD_NO_PERM("command.no-permission", Type.ERROR),
    CMD_ONLY_PLAYERS("command.only-players", Type.ERROR),
    CMD_ERROR_DYNAMIC("command.dynamic-error", Type.ERROR), // Generic error wrapper

    CMD_HELP_MACRO("command.help-macro", Type.RAW),

    // Parser & Args Errors
    CMD_INVALID_ARG("command.invalid-arg", Type.ERROR),
    CMD_UNKNOWN_MAT("command.unknown-material", Type.ERROR),

    CMD_RELOAD_SUCCESS("command.reload-success", Type.RAW),

    // ==========================================
    // 2. IO & SCHEMATICS
    // ==========================================
    // Basic Load/Save
    SCHEM_LOAD_SUCCESS("listener.schem.load-success", Type.SUCCESS),
    SCHEM_LOAD_ERROR("listener.schem.load-error", Type.ERROR),
    SCHEM_SAVE_SUCCESS("listener.schem.save-success", Type.SUCCESS),
    SCHEM_FORMAT_ERROR("listener.schem.unknown-format", Type.ERROR),
    SCHEM_NOT_FOUND("listener.schem.not-found", Type.ERROR),
    IO_DISK_FULL("io.disk-full", Type.ERROR),

    // Transcoding
    IO_TRANSCODE_STARTING("io.transcode-starting", Type.INFO),
    IO_TRANSCODE_NO_SCHEMATICS("io.transcode-no-schematics", Type.ERROR),
    IO_TRANSCODE_FAILED_ALL("io.transcode-failed-all", Type.ERROR),

    // File Operations
    IO_FILE_EXISTS("io.file-exists", Type.ERROR),
    IO_FOLDER_EXISTS("io.folder-exists", Type.ERROR),
    IO_SAVE_ERROR("io.save-error", Type.ERROR),
    IO_SAVED_SINGLE("io.saved-single", Type.SUCCESS),
    IO_SAVED_COUNT("io.saved-count", Type.SUCCESS),
    IO_FAV_SAVED("io.fav-saved", Type.SUCCESS),

    // Web / Network
    SCHEM_DOWNLOADING("listener.schem.downloading", Type.INFO),
    SCHEM_UPLOADING("listener.schem.uploading", Type.INFO),
    SCHEM_UPLOAD_SUCCESS("listener.schem.upload-success", Type.SUCCESS),
    SCHEM_UPLOAD_FAILED("listener.schem.upload-failed", Type.ERROR),
    SCHEM_UPLOAD_ERROR("listener.schem.upload-error", Type.ERROR),

    // Usage Help
    SCHEM_SAVE_USAGE("listener.schem.save-usage", Type.ERROR),
    SCHEM_LOAD_USAGE("listener.schem.load-usage", Type.ERROR),

    // ==========================================
    // 3. TRANSFORM & MATH
    // ==========================================
    TRANSFORM_SUCCESS("listener.transform-success", Type.SUCCESS),
    TRANSFORM_NO_OBJECTS("listener.no-objects-transform", Type.ERROR),
    TRANSFORM_INVALID_DIR("listener.transform-invalid-dir", Type.ERROR),
    TRANSFORM_USAGE_ROTATE("listener.transform-usage-rotate", Type.ERROR),
    TRANSFORM_NUMBER_ERROR("listener.transform-number-error", Type.ERROR),

    // Grammar Parts (RAW)
    TRANSFORM_OBJ_S("listener.transform-singular", Type.RAW),
    TRANSFORM_OBJ_P("listener.transform-plural", Type.RAW),
    TRANSFORM_ACT_ROT_S("listener.transform-action-rotated-s", Type.RAW),
    TRANSFORM_ACT_ROT_P("listener.transform-action-rotated-p", Type.RAW),
    TRANSFORM_ACT_FLIP_S("listener.transform-action-flipped-s", Type.RAW),
    TRANSFORM_ACT_FLIP_P("listener.transform-action-flipped-p", Type.RAW),

    // ==========================================
    // 4. REGION & SELECTION
    // ==========================================
    REGION_COMPLETE("region.complete", Type.SUCCESS),
    REGION_TOO_LARGE("region.too-large", Type.ERROR),
    REGION_POS_SET("region.pos-set", Type.INFO),
    REGION_SEL_REQUIRED("region.selection-required", Type.ERROR),
    REGION_GEN_ERROR("region.error", Type.ERROR),

    // OSN & POS Control
    REGION_OSN_SUCCESS("region.osn.success", Type.SUCCESS),
    REGION_POS_SUCCESS("region.pos.success", Type.SUCCESS),

    // Parser Specific
    CMD_INVALID_NUMBER("region.invalid-number", Type.ERROR),
    CMD_EMPTY_HAND("region.empty-hand", Type.ERROR),
    CMD_EMPTY_SLOT("region.empty-slot", Type.ERROR),
    CMD_OSN_USAGE("region.osn.usage", Type.ERROR),

    // ==========================================
    // 5. COPY & PASTE SERVICE
    // ==========================================
    // Errors
    COPY_ALREADY_ACTIVE("copy.already-copying", Type.ERROR),
    COPY_SELECTION_REQ("copy.selection-required", Type.ERROR),
    COPY_TOO_LARGE("copy.too-large", Type.ERROR),
    COPY_MASK_ERROR("copy.mask-error", Type.ERROR),
    COPY_ERROR("copy.copy-error", Type.ERROR),

    // Magic Copy (Batch)
    MAGIC_COPY_SUCCESS_BRUSH("brush.cp.magic-copy.success-brush", Type.SUCCESS),
    MAGIC_COPY_SUCCESS_GLOBAL("brush.cp.magic-copy.success-global", Type.SUCCESS),
    MAGIC_COPY_DETAILS("brush.cp.magic-copy.details", Type.RAW),

    // Success Composition (Parts)
    COPY_SUCCESS_CUT("copy.success.cut", Type.SUCCESS),
    COPY_SUCCESS_COPIED("copy.success.copied", Type.SUCCESS),
    COPY_SUCCESS_BUFFER("copy.success.to-buffer", Type.SUCCESS), // Used as RAW sometimes

    // RAW Parts for composition
    COPY_SUCCESS_TO_BRUSH("copy.success.to-brush", Type.RAW),
    COPY_SUCCESS_ANCHOR("copy.success.anchor", Type.RAW),
    COPY_SUCCESS_ENTITIES("copy.success.entities", Type.RAW),
    COPY_SUCCESS_BIOMES("copy.success.biomes", Type.RAW),
    COPY_SUCCESS_DETAILS("copy.success.details", Type.RAW),

    // Paste
    PASTE_ERROR("paste.error-item", Type.ERROR),
    PASTE_SUCCESS("paste.success-item", Type.SUCCESS),
    PASTE_GROUP_EMPTY("paste.group-empty", Type.ERROR),
    PASTE_MASK_ERROR("paste.error-mask", Type.ERROR),
    PASTE_SEL_UPDATED("paste.selection-updated", Type.INFO),

    // Wait
    SCHEM_WAIT_COPY("listener.wait-copy", Type.INFO),

    // ==========================================
    // 6. BUFFER & LISTS
    // ==========================================
    BUFFER_EMPTY("buffer.empty", Type.INFO),
    BUFFER_UPDATED("buffer.updated-single", Type.SUCCESS),
    BUFFER_ADDED_SINGLE("buffer.added-single", Type.SUCCESS),

    // List Display (RAW)
    BUFFER_LIST_HEADER("buffer.list-header", Type.RAW),
    BUFFER_MODE_RANDOM("buffer.mode-random", Type.RAW),
    BUFFER_MODE_SELECTED("buffer.mode-selected", Type.RAW),
    BUFFER_PREFIX_SELECTED("buffer.prefix-selected", Type.RAW),
    BUFFER_PREFIX_NORMAL("buffer.prefix-normal", Type.RAW),

    // Suffixes (RAW)
    BUFFER_SUFFIX_BRUSH("buffer.added-single-brush-suffix", Type.RAW),
    BUFFER_SUFFIX_NORMAL("buffer.added-single-suffix", Type.RAW),
    BUFFER_ITEM_IN_MEMORY("buffer.item.in-memory", Type.RAW),
    BUFFER_ITEM_BLOCKS("buffer.item.blocks", Type.RAW),
    BUFFER_ITEM_GROUP_PREFIX("buffer.item.group-prefix", Type.RAW),
    BUFFER_ITEM_COUNT_SUFFIX("buffer.item.count-suffix", Type.RAW),

    // Context & Loading
    BUFFER_NO_MEMORY_OBJS("buffer.no-memory-objects", Type.INFO),
    BUFFER_CONTEXT_BRUSH("buffer.context-brush", Type.RAW),
    BUFFER_CONTEXT_GLOBAL("buffer.context-global", Type.RAW),
    BUFFER_GROUP_LOADED("buffer.group-loaded", Type.SUCCESS),
    BUFFER_TARGET_BRUSH("buffer.target-brush", Type.RAW),
    BUFFER_ACTION_BAR("buffer.action-bar-selection", Type.RAW),

    // GUI Limits
    GUI_LIMIT_BUFFER("gui.brush-list.errors.limit-buffer", Type.ERROR),

    // ==========================================
    // 7. BRUSH CONTROLLER
    // ==========================================
    BRUSH_BIND_AIR("brush.controller.cant-bind-air", Type.ERROR),
    BRUSH_CANT_USE("brush.controller.cant-use-item", Type.ERROR),
    BRUSH_UNBIND("brush.controller.unbind", Type.INFO),
    BRUSH_COPY_EQUIPPED("brush.controller.copy-equipped", Type.SUCCESS),
    BRUSH_CP_BOUND("brush.controller.cp-bound", Type.SUCCESS),
    BRUSH_CP_ERROR_ARGS("brush.controller.cp-error", Type.ERROR),
    BRUSH_WAIT_GENERATION("brush.wait-generation", Type.INFO),

    MACRO_NOT_BOUND("...", Type.INFO),
    BRUSH_MACRO_ACTIVE("...", Type.INFO),

    BRUSH_BOUND("brush.controller.bound", Type.INFO),
    BRUSH_TOOL_BOUND("brush.controller.tool-bound", Type.INFO),
    BRUSH_SIZE_SET("brush.controller.size-set", Type.INFO),
    BRUSH_NO_SIZE("brush.controller.no-size", Type.ERROR),

    // Procedural Activation
    BRUSH_PLUMP_ACTIVE("brush.controller.plump-active", Type.SUCCESS),
    BRUSH_GEN_ACTIVE("brush.controller.gen-active", Type.SUCCESS),
    BRUSH_CRYSTAL_ACTIVE("brush.controller.crystal-active", Type.SUCCESS),
    BRUSH_GEN_FORMULA_ERROR("brush.gen.syntax-error", Type.ERROR),

    // Editor Activation
    BRUSH_EDITOR_ACTIVE("brush.controller.editor-activated", Type.SUCCESS),
    BRUSH_EDITOR_HELP("brush.controller.editor-help", Type.RAW),
    BRUSH_PASTEBIN_REQ("brush.controller.pastebin-required", Type.ERROR),

    // JSON Export
    BRUSH_JSON_COPY("brush.controller.json-copy", Type.RAW),
    BRUSH_JSON_HOVER("brush.controller.json-hover", Type.RAW),

    // ==========================================
    // 8. BRUSH LOGIC (Execution)
    // ==========================================
    BRUSH_CP_ERROR("brush.cp.error-generic", Type.ERROR),
    BRUSH_CP_EMPTY("brush.cp.buffer-empty", Type.ERROR),
    BRUSH_TOO_LARGE("brush.cp.too-large", Type.ERROR),
    BRUSH_POS_ERROR("brush.cp.pos-error", Type.ERROR),
    BRUSH_LOAD_ERROR("brush.cp.load-error", Type.ERROR),
    BRUSH_GROUP_NOT_FOUND("brush.controller.group-not-found", Type.ERROR),
    BRUSH_GROUP_EMPTY("brush.cp.group-empty", Type.ERROR),

    BRUSH_CP_FILE_MISSING("brush.cp.file-missing", Type.ERROR),
    BRUSH_CP_GENERIC_ERROR("brush.cp.generic-error", Type.ERROR),

    // Selection Logic
    BRUSH_SEL_TOO_LARGE("brush.selection.too-large", Type.ERROR),
    BRUSH_SEL_TOO_LARGE_ADD("brush.selection.too-large-add", Type.ERROR),
    BRUSH_SEL_SELECTED("brush.selection.selected", Type.SUCCESS),
    BRUSH_SEL_EXPANDED("brush.selection.expanded", Type.SUCCESS),
    BRUSH_SEL_ERROR("brush.selection.error-set", Type.ERROR),

    // Magic Copy Internal
    BRUSH_MAGIC_INTERNAL_ERROR("brush.cp.internal-error", Type.ERROR),
    BRUSH_MAGIC_SEARCH_TIMEOUT("brush.cp.search-timeout", Type.ERROR),
    BRUSH_MAGIC_TOO_LARGE("brush.cp.too-large", Type.ERROR),

    // Clear Buffer
    BRUSH_CLEAR_BRUSH("brush.cp.clear.brush", Type.SUCCESS),
    BRUSH_CLEAR_GLOBAL("brush.cp.clear.global", Type.SUCCESS),
    BRUSH_CLEAR_ALL("brush.cp.clear.all", Type.SUCCESS),

    // Action Bar
    BRUSH_ACTION_ADDED("brush.cp.action-bar.added", Type.RAW),
    BRUSH_ACTION_EMPTY("brush.cp.action-bar.empty", Type.RAW),
    BRUSH_ACTION_ROTATION("brush.cp.action-bar.rotation", Type.RAW),

    // ==========================================
    // 9. CONSTRUCT EDITOR
    // ==========================================
    // Actions
    CONSTRUCT_CENTER_SET("construct.editor.center-set", Type.SUCCESS),
    CONSTRUCT_POINT_ADDED("construct.editor.point-added", Type.SUCCESS),
    CONSTRUCT_POINT_REMOVED_SEL("construct.editor.point-removed-selected", Type.INFO),
    CONSTRUCT_POINT_REMOVED_LAST("construct.editor.point-removed-last", Type.INFO),
    CONSTRUCT_POINT_ROTATED("construct.editor.point-rotated", Type.INFO),
    CONSTRUCT_SELECTED("construct.editor.selected", Type.INFO),
    CONSTRUCT_DESELECTED("construct.editor.deselected", Type.INFO),
    CONSTRUCT_CENTER_MOVED("construct.editor.center-moved", Type.INFO),
    CONSTRUCT_POINT_MOVED("construct.editor.point-moved", Type.INFO),
    CONSTRUCT_CLEARED("construct.editor.cleared", Type.INFO),

    // Naming
    CONSTRUCT_POINT_NAME_CENTER("construct.editor.point-name-center", Type.RAW),
    CONSTRUCT_POINT_NAME_NUMBERED("construct.editor.point-name-numbered", Type.RAW),

    // Limits & Errors
    CONSTRUCT_LIMIT_POINTS("construct.editor.limit-points", Type.ERROR),
    CONSTRUCT_TOO_FAR("construct.editor.too-far", Type.ERROR),
    CONSTRUCT_TOO_CLOSE_CENTER("construct.editor.too-close-center", Type.ERROR),
    CONSTRUCT_TOO_CLOSE_POINT("construct.editor.too-close-point", Type.ERROR),
    CONSTRUCT_NO_POINTS("construct.editor.no-points", Type.ERROR),
    CONSTRUCT_SAVE_EMPTY("construct.editor.save-empty", Type.ERROR),
    CONSTRUCT_SAVE_NO_CENTER("construct.editor.save-no-center", Type.ERROR),
    CONSTRUCT_LIMIT_GROUPS("construct.editor.limit-groups", Type.ERROR),

    // Saving
    CONSTRUCT_SAVE_SUCCESS("construct.editor.save-success", Type.SUCCESS),
    CONSTRUCT_SAVE_INFO("construct.editor.save-info", Type.INFO),

    // ==========================================
    // 10. SCROLL CONTROLLER
    // ==========================================
    // Action Bar Messages (RAW)
    SCROLL_MODE_CHANGED("scroll.mode-changed", Type.RAW),
    SCROLL_MODE_RESET("scroll.mode-reset", Type.RAW),
    SCROLL_SCALE("scroll.scale", Type.RAW),
    SCROLL_RADIUS("scroll.radius", Type.RAW),
    SCROLL_SCALE_AXIS("scroll.scale-axis", Type.RAW),

    // Rotation Parts (RAW)
    SCROLL_ROTATION_BASE("scroll.rotation", Type.RAW),
    SCROLL_AXIS_Y("scroll.axis-y", Type.RAW),
    SCROLL_AXIS_X("scroll.axis-x", Type.RAW),
    SCROLL_AXIS_Z("scroll.axis-z", Type.RAW),

    // Action Names (RAW)
    ACTION_SCALE("scroll.action.scale", Type.RAW),
    ACTION_SCALE_AXIS("scroll.action.scale-axis", Type.RAW),
    ACTION_ROTATION("scroll.action.rotation", Type.RAW),
    ACTION_BUFFER_SLOT("scroll.action.buffer-slot", Type.RAW),
    ACTION_RADIUS("scroll.action.radius", Type.RAW),

    // ==========================================
    // 11. SHORTCUTS
    // ==========================================
    SHORTCUT_HELP("shortcut.help", Type.INFO),
    SHORTCUT_DELETE_USAGE("shortcut.delete-usage", Type.ERROR),
    SHORTCUT_DELETED("shortcut.deleted", Type.SUCCESS),

    // Limits
    MACRO_NO_PERM("shortcut.no-permission", Type.ERROR),
    MACRO_LIMIT_REACHED("shortcut.limit-reached", Type.ERROR),
    MACRO_CMD_LIMIT("shortcut.cmd-limit", Type.ERROR),
    MACRO_CMD_LIMIT_SERVER("shortcut.cmd-limit-server", Type.RAW),
    MACRO_FORBIDDEN_CMD("shortcut.forbidden-cmd", Type.ERROR),

    // Recording
    MACRO_REC_ADDED("shortcut.rec.added", Type.RAW),
    MACRO_REC_SAVED("shortcut.rec.saved", Type.SUCCESS),
    MACRO_ACTION_BAR("shortcut.action-bar", Type.RAW),
    MACRO_RECURSION("shortcut.recursion-error", Type.ERROR),

    // ==========================================
    // 12. PATTERN PARSERS (Injection)
    // ==========================================
    PARSER_PLAYERS_ONLY("parser.players-only", Type.ERROR),
    PARSER_BUFFER_EMPTY("parser.buffer-empty", Type.ERROR),
    PARSER_READ_ERROR("parser.read-error", Type.ERROR),
    PARSER_SCHEM_NOT_FOUND("parser.schem-not-found", Type.ERROR),
    PARSER_INTERNAL_ERROR("parser.internal-error", Type.ERROR),

    // ==========================================
    // 13. PROMPTS & INPUT
    // ==========================================
    PROMPT_ACTION_BAR("prompt.action-bar", Type.RAW),


    // ==========================================
    // 14. NETWORK & IMPORT
    // ==========================================
    NETWORK_DOWNLOADING("network.downloading", Type.INFO),
    NETWORK_HTTP_ERROR("network.http-error", Type.ERROR),
    NETWORK_FILE_TOO_LARGE("network.file-too-large", Type.ERROR),
    NETWORK_IMPORT_SUCCESS("network.import-success", Type.SUCCESS),
    NETWORK_INVALID_JSON("network.invalid-json", Type.ERROR),
    NETWORK_DOWNLOAD_ERROR("network.download-error", Type.ERROR),

// clipboard gui

    GUI_CLIP_GROUP_UNPACKED("gui.clipboard.group-unpacked", Type.INFO),
    GUI_CLIP_GROUP_COPY_ERROR("gui.clipboard.group-copy-error", Type.ERROR),
    GUI_CLIP_FOLDER_NOT_EMPTY("gui.clipboard.folder-not-empty", Type.ERROR),
    GUI_CLIP_DUPLICATE_IGNORED("gui.clipboard.duplicate-ignored", Type.ERROR),
    GUI_CLIP_CLEARED("gui.clipboard.cleared", Type.SUCCESS),

    // construct
    CONSTRUCT_EXPORT_HOVER("construct.editor.export-hover", Type.RAW),
    CONSTRUCT_EXPORT_CLICK("construct.editor.export-click", Type.RAW),
    CONSTRUCT_EXPORT_ERROR("construct.editor.export-error", Type.ERROR),

    //material gui
    GUI_LIMIT_REACHED("gui.common.limit-reached", Type.ERROR),
    GUI_MAT_EXISTS("gui.common.material-exists", Type.ERROR),
    GUI_MAT_PROMPT("gui.common.material-prompt", Type.INFO);



    private final String key;
    private final Type type;

    OrionMessage(String key, Type type) {
        this.key = key;
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        ERROR, SUCCESS, INFO, RAW
    }
}