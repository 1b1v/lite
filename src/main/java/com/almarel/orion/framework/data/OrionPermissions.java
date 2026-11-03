package com.almarel.orion.framework.data;

/**
 * Реестр всех прав доступа (Permissions) плагина 1b1v.
 * Структурирован по логическим модулям для удобства управления.
 */
public final class OrionPermissions {

    private OrionPermissions() {}

    // --- CORE (Общие права плагина) ---

    /**
     * Floor-gate для административных команд с side-эффектами (сейчас: /1b1v reload).
     * Отдельно от ADMIN_NOTICE (там — пассивная подписка, без риска).
     * @category ADMIN (Blast Radius: Server; только 1b1v.admin)
     */
    public static final String ADMIN_USE = "1b1v.admin.use";

    /**
     * Доступ к login-уведомлениям и связанным действиям (обновления, установка пресетов,
     * синхронизация ассетов).
     * @category ADMIN (Blast Radius: Server; только 1b1v.admin)
     */
    public static final String ADMIN_NOTICE = "1b1v.admin.notice";

    /**
     * Публикация/удаление глобальных пресетов (кисти, конструкты, макросы — единый JSON-реестр).
     * НЕ включает CLIPBOARD_GLOBAL (другая категория риска — прямой I/O в хранилище схем FAWE).
     * @category ADMIN (Blast Radius: Server; только 1b1v.admin)
     */

    /**
     * Floor-gate: базовый доступ к дереву команд /1b1v целиком (без него сама
     * команда не отвечает игроку, кроме сообщения "нет прав"). Проверяется
     * ОДИН РАЗ централизованно в OrionCommand.hasBasicAccess(), а не в каждом
     * сабкоманде отдельно — сабкоманды, которым не нужна доменная специфика
     * сверх floor'а, возвращают getPermission() == null, т.к. проверка CORE_USE
     * здесь уже избыточна.
     * Доступ к самим GUI-доменам (Clipboard/Macro/...) НЕ регулируется этим
     * правом — гейтится отдельно, в точке реального открытия меню внутри
     * GuiWindowManager (см. CLIPBOARD_USE, MACRO_USE), что защищает и от
     * обхода через команду, и от обхода через клик по навбару.
     * @category BASIC (Blast Radius: Self — само по себе не исполняет никакое
     * действие с миром и не открывает ни один GUI-домен)
     */
    public static final String CORE_USE = "1b1v.use";

    // --- ALIASES & SYNTAX ---
    // SINGLE_SLASH_USE функционально ЗАВИСИТ от ALIASES_USE (AliasRoutingService.resolveAlias,
    // ранний выход) — не работает без него, даже если выдано отдельно.

    /** Доступ к алиасам команд (//c, //p и т.д.).
     * @category BASIC (Blast Radius: Self — транслирует в исходную команду, та проверяется своими правами) */

    /** Синтаксис с одним слешем (/set вместо //set). Требует ALIASES_USE (см. выше по секции).
     * @category BASIC (Performance — O(1) string preprocessing; Blast Radius: Self — не открывает
     * новый функционал, только сокращённый синтаксис вызова существующих команд) */

    // --- HISTORY ---

    /** Доступ к истории команд и её повторению (//lc, //.).
     * @category BASIC (Blast Radius: Self — повтор проходит обычную permission-проверку заново) */

    // --- ID SYSTEM ---

    /** Отображение ID блоков (Legacy/Custom) в Action Bar.
     * @category BASIC (Performance — O(1) lookup + единичный event handler, без растущих структур;
     * Blast Radius: Self — чисто информационный action bar) */

    /** Каталог блоков по ID (/1b1v blockid) — give-инструмент, клик выдаёт стак 64 бесплатно.
     * Рассчитан на creative/build-сервера (на survival — economy-bypass).
     * @category BASIC (Performance — O(1) чтение готового кэша; тяжёлое построение кэша асинхронно
     * и однократно при старте, не зависит от количества игроков с правом) */

    // --- BRUSH (Кисти) ---

    /** Кисть сглаживания рельефа.
     * @category INTERMEDIATE (async blur) */

    /** Кисть копирования-вставки (/br cp).
     * @category INTERMEDIATE (RAM — Copy-on-Write detachment per transform) */

    /** Инструмент выделения области для копирования (/br copy).
     * @category INTERMEDIATE (flood-scan объёма) */

    /** Процедурная кисть Plump (наращивание/утолщение форм).
     * @category INTERMEDIATE (параллельный voxel-scan через AsyncExecutor, блокирует Main Thread
     * на время build() — системное свойство FAWE BrushTool.act(), не специфика кисти; объём скана
     * на максимальном радиусе ~93.6M вокселей — меньше MAX_VOLUME=100M у MASK_AROUND при в 15 раз
     * большем допустимом там радиусе, что и держит категорию Intermediate, а не Advanced) */
    public static final String BRUSH_PLUMP_USE = "1b1v.brush.plump";

    /** Процедурная кисть Formula (генерация по математическим формулам).
     * @category INTERMEDIATE (движок как у BRUSH_PLUMP_USE + ThreadLocal-компиляция Expression) */

    /** Процедурная кисть Crystal.
     * @category INTERMEDIATE (движок как у BRUSH_PLUMP_USE) */
    public static final String BRUSH_CRYSTAL_USE = "1b1v.brush.crystal";

    /** Кисть редактирования существующих конструктов.
     * @category INTERMEDIATE (ограничена CONSTRUCT_POINTS_LIMIT_PREFIX; только Pro) */

    /** Кисть выполнения записанных макросов (/br macro).
     * @category INTERMEDIATE (риск зависит от содержимого — см. MACRO_CREATE_*) */

    /** Разрешает кистям использовать персональный независимый буфер (сейчас: CopyPaste, /br cp).
     * @category BASIC (bounded лимитом BRUSH_CLIPBOARD_LIMIT_PREFIX, максимум 8) */

    /** Префикс лимита количества кистей с персональным буфером (напр. 1b1v.brush.clipboard.limit.1). */

    // ========================================================
    // --- ARCEON BRIDGE (Права для кистей стороннего плагина Arceon) ---
    // Намеренно НЕ объявлены в plugin.yml (ADL №7 — на админе; незарегистрированная
    // Bukkit-нода по умолчанию доступна только op). Без @category.
    // ========================================================

    /** Право на кисть BoulderBrush (Arceon, через рефлексию). */
    public static final String ARCEON_BOULDER = "arceon.brush.boulder";

    /** Право на кисть CropBrush (Arceon, через рефлексию). */
    public static final String ARCEON_CROP = "arceon.brush.crop";

    /** Право на кисть CubeBrush (Arceon, через рефлексию). */
    public static final String ARCEON_CUBE = "arceon.brush.cube";

    /** Право на кисть ErodeBlendBrush (Arceon, через рефлексию). */
    public static final String ARCEON_ERODE_BLEND = "arceon.brush.erodeblend";

    /** Право на инструмент RevolveBrush (Arceon, через рефлексию). */
    public static final String ARCEON_REVOLVE = "arceon.tool.revolve";

    /** Право на кисть SpikeBrush (Arceon, через рефлексию). */
    public static final String ARCEON_SPIKE = "arceon.brush.spike";

    /** Право на кисть VineBrush (Arceon, через рефлексию). */
    public static final String ARCEON_VINE = "arceon.brush.vine";

    // ========================================================
    // --- FAWE TOOL BRIDGE (чужие ноды FAWE/WorldEdit для native tools) ---
    // Настраиваются только через FAWE/WorldEdit, повторная настройка в 1b1v не нужна.
    // ========================================================

    /** Право FAWE на инструмент циклического изменения данных блока (//cycler). */
    public static final String FAWE_TOOL_DATA_CYCLER = "worldedit.tool.data-cycler";

    /** Право FAWE на инструмент дальнего выделения (//farwand). */
    public static final String FAWE_TOOL_FARWAND = "worldedit.tool.farwand";

    /** Право FAWE на инструмент заливки (//floodfill). */
    public static final String FAWE_TOOL_FLOOD_FILL = "worldedit.tool.flood-fill";

    /** Право FAWE на информационный инструмент (//info). */
    public static final String FAWE_TOOL_INFO = "worldedit.tool.info";

    /** Право FAWE на инструмент инспекции блоков (//inspect). */
    public static final String FAWE_TOOL_INSPECT = "worldedit.tool.inspect";

    /** Право FAWE на инструмент строительства на дистанции (//lrbuild). */
    public static final String FAWE_TOOL_LRBUILD = "worldedit.tool.lrbuild";

    /** Право FAWE на инструмент замены блоков кликом (//repl). */
    public static final String FAWE_TOOL_REPLACER = "worldedit.tool.replacer";

    /** Право FAWE на инструмент стакания блоков в направлении взгляда (//stacker). */
    public static final String FAWE_TOOL_STACK = "worldedit.tool.stack";

    /** Право FAWE на инструмент посадки деревьев кликом (//tree). */
    public static final String FAWE_TOOL_TREE = "worldedit.tool.tree";

    // ========================================================
    // --- FAWE BRIDGE (чужие ноды FAWE, домен Clipboard) ---
    // ========================================================

    /** Право FAWE на //schematic save. WorldEdit принимает OR с FAWE_CLIPBOARD_SAVE —
     * проверяются обе ноды (SchematicSaveHandler). */

    /** Альтернативное (OR) право на сохранение, см. FAWE_SCHEM_SAVE. */

    /** Право FAWE на //schematic load. OR с FAWE_CLIPBOARD_LOAD (SchematicLoadHandler). */

    /** Альтернативное (OR) право на загрузку, см. FAWE_SCHEM_LOAD. */

    /** Право FAWE на //paste. */

    /** Право FAWE на //rotate. */

    /** Право FAWE на //flip. */

    /** Право FAWE на //copy. */

    /** Право FAWE на //cut. */

    /** Право FAWE на //clearclipboard. OR с FAWE_SCHEM_CLEAR (ClearClipboardCommand). */

    /** Альтернативное (OR) право на очистку, см. FAWE_CLIPBOARD_CLEAR. */

    /** Право FAWE на публикацию буфера во внешний веб-сервис (//schematic share, Orion /download).
     * OR с FAWE_SCHEM_SHARE. */

    /** Альтернативное (OR) право, см. FAWE_CLIPBOARD_SHARE. */

    /** Право FAWE на загрузку схемы по веб-ссылке (//schem load url:<uuid>; также используется
     * Orion-загрузкой по http(s), ограниченной доверенным хостом FAWE). */

    /** Право FAWE на обход изоляции PER_PLAYER_SCHEMATICS — просмотр/загрузка схем других
     * игроков по прямому пути (UUID в пути), когда персональные папки включены.
     * Используется в SchematicIndexService/PatternSourceResolver как defensive bypass-check,
     * калька логики UtilityCommands.getFiles() из FAWE. */
    public static final String FAWE_SCHEM_LIST_OTHER = "worldedit.schematic.list.other";

    /** Право FAWE на //schematic delete. Без OR-альтернативы (в отличие от save/load).
     * Используется ТОЛЬКО для гейтинга Orion-side автодополнения путей (Block C roadmap,
     * closing UX/visibility gap) — само удаление исполняет нативный FAWE-пайплайн,
     * SchematicCommand.process() для "delete" не отменяет событие (return false намеренно).
     * Owner-aware фильтрация подсказок обеспечивается SchematicSuggestionService через
     * уже существующий механизм (SchematicIndexService), PER_PLAYER_SCHEMATICS учитывается
     * прозрачно. */
    public static final String FAWE_SCHEM_DELETE = "worldedit.schematic.delete";

    // ========================================================
    // --- FAWE BRUSH BRIDGE (чужие ноды FAWE для native brushes) ---
    // ========================================================

    /** Право FAWE на //brush cylinder (и hcyl, HollowCylinderBrush). */
    public static final String FAWE_BRUSH_CYLINDER = "worldedit.brush.cylinder";

    /** Право FAWE на //brush blendball (FAWE-специфичная кисть). */
    public static final String FAWE_BRUSH_BLENDBALL = "worldedit.brush.blendball";

    /** Право FAWE на BlobBrush. ВНИМАНИЕ: нода "rock", не "blob" — так зашито в самом FAWE. */
    public static final String FAWE_BRUSH_ROCK = "worldedit.brush.rock";

    /** Право FAWE на //brush sphere. Общее для SphereBrushProcessor и CircleBrushProcessor
     * (в FAWE обе кисти под одной нодой). */
    public static final String FAWE_BRUSH_SPHERE = "worldedit.brush.sphere";

    /** Право FAWE на FlattenBrush (Cliff-процессор). ВНИМАНИЕ: нода "height", не "heightmap",
     * как указано в официальной wiki WorldEdit. */
    public static final String FAWE_BRUSH_HEIGHT = "worldedit.brush.height";

    /** Право FAWE на ErodeBrush. ВНИМАНИЕ: нода "erode", не "morph" (dilate/erode делят одно
     * право по вики WorldEdit). */
    public static final String FAWE_BRUSH_ERODE = "worldedit.brush.erode";

    /** Право FAWE на //brush extinguish. */
    public static final String FAWE_BRUSH_EXTINGUISH = "worldedit.brush.ex";

    /** Право FAWE на //brush gravity. */
    public static final String FAWE_BRUSH_GRAVITY = "worldedit.brush.gravity";

    /** Право FAWE на LineBrush (FAWE-специфичная кисть). */
    public static final String FAWE_BRUSH_LINE = "worldedit.brush.line";

    /** Право FAWE на SplineBrush (FAWE-специфичная кисть). */
    public static final String FAWE_BRUSH_SPLINE = "worldedit.brush.spline";

    /** Право FAWE на SurfaceSphereBrush (FAWE-специфичная кисть). */
    public static final String FAWE_BRUSH_SURFACE = "worldedit.brush.surface";

    /**
     * Нативное право FAWE на //smooth. Orion замещает команду, проверяет ТУ ЖЕ ноду
     * (подсветка = исполнение). Прокси в #smooth (PATTERN_SMOOTH), тот же тир (advanced).
     * Также требует worldedit.region.replace. БЕЗ top-level в plugin.yml.
     * Вырезано в Lite (там //smooth нативный).
     */

    // --- CLIPBOARD (Буферы и Лимиты) ---

    /** Главный рубильник системы буферов (необходим для //copy, //paste).
     * @category BASIC (тяжесть действий гейтится отдельными правами — BRUSH_COPY_USE, PATTERN_*) */

    /** Доступ к вкладке Shared Groups (просмотр, инвайт, скачивание). Любой участник может
     * удалять файлы в общей папке — доверительная модель (ADL №1).
     * @category INTERMEDIATE (Blast Radius: Shared) */

    /** Создание Shared-групп.
     * @category ADVANCED (DISK+DB, unbounded без CLIPBOARD_SHARED_GROUPS_LIMIT_PREFIX) */

    /** Префикс лимита Shared-групп на владельца (напр. limit.3). Enforcement:
     * SharedGroupsModule → SessionLimitService.resolveSharedGroupsLimit() (дефолт 1). */

    /** Доступ к вкладке Favorites.
     * @category BASIC (bounded лимитом CLIPBOARD_FAVORITES_LIMIT_PREFIX, дефолт 20) */

    /** Префикс лимита Favorites-схем, рекурсивно по подпапкам (напр. limit.20). Enforcement:
     * ExportService → SessionLimitService.resolveFavoritesLimit() (дефолт 20). */

    /**
     * Префикс лимита схем в ОДНОМ буфере — общий для главного буфера игрока и персонального
     * буфера каждой кисти (напр. limit.6). Действует и live (BufferLifecycleService), и после
     * рестарта (SessionSnapshotMapper). НЕ путать с BRUSH_CLIPBOARD_LIMIT_PREFIX (тот считает
     * количество кистей, этот — схемы внутри буфера). Enforcement: SessionLimitService →
     * SessionClipboardData.maxClipboardSlots (дефолт 1).
     */

    /** Запись в ОБЩИЙ корень схем FAWE (//schem save/saveall, экспорт из FAWE Schematics GUI).
     * -f перезаписывает чужие файлы — только для админов. Игроки сохраняют через Favorites.
     * @category ADMIN (Blast Radius: Server; только 1b1v.admin) */

    // --- CONSTRUCTS ---

    /** Доступ к системе конструктов (меню, навигация, выбор групп).
     * @category INTERMEDIATE (async visualizer с фантомными блоками; снижено Delta Check + лимитом точек) */

    /** Импорт конструктов по URL.
     * @category ADVANCED (SSRF — исходящий GET по произвольному URL с отключённой проверкой SSL;
     * DoS закрыт лимитом MAX_DOWNLOAD_CHARS=16000 и таймаутом 5с, SSRF-риск остаётся) */

    /** Префикс лимита групп конструктов на игрока (напр. limit.5). */

    /** Префикс лимита точек привязки в одной группе (напр. limit.10). */

    // --- SELECTION ---

    /** Команда //pos12.
     * @category BASIC (O(1) — CuboidRegionSelector, без итерации блоков) */

    /** Команда //osn — куб вокруг игрока заданного радиуса/высоты (аналог //outset, но от игрока).
     * @category BASIC (та же математика, что SELECTION_POSBOTH) */

    // --- REGION ---

    /** Команда //mirror.
     * @category INTERMEDIATE (RAM-клон региона в BlockArrayClipboard перед трансформацией;
     * доп. хардкап объёма сверх worldedit.limit — см. MAX_REGION_VOLUME в MirrorCommand) */

    /** Команда //frame.
     * @category BASIC (O(1) per-block edge-test, без auxiliary-структур; governed worldedit.limit) */
    public static final String REGION_FRAME = "1b1v.region.frame";

    /** Команда //polygon.
     * @category BASIC (O(1) per-block SDF-проверка; governed worldedit.limit) */
    public static final String REGION_POLYGON = "1b1v.region.polygon";

    /** Команда //centerver.
     * @category BASIC (O(1) per-block write, без auxiliary-структур; не верифицировано чтением
     * CenterVerticalGenerationService) */
    public static final String REGION_CENTER_VERTICAL = "1b1v.region.centervertical";

    // --- GENERATION ---

    /** Команда //platform.
     * @category BASIC (O(1) per-block write; governed worldedit.limit) */
    public static final String GENERATION_PLATFORM = "1b1v.generation.platform";

    /** Команда //onion.
     * @category BASIC (O(1) per-block SDF-проверка; governed worldedit.limit) */
    public static final String GENERATION_ONION = "1b1v.generation.onion";

    // --- CURVE ---

    /** Команда //bridge.
     * @category INTERMEDIATE (непрерывная sweep-структура вдоль пути) */

    /** Команда //liana.
     * @category INTERMEDIATE (движок CurvePathService, как CURVE_BRIDGE) */
    public static final String CURVE_LIANA = "1b1v.curve.liana";

    /** Команда //ribbon.
     * @category INTERMEDIATE (движок CurvePathService, как CURVE_BRIDGE) */

    /** Команда //stream.
     * @category INTERMEDIATE (движок CurvePathService, как CURVE_BRIDGE) */

    // --- PATTERNS ---

    /** Паттерн #ridgedmulti.
     * @category BASIC (O(1) noise sampling per block) */
    public static final String PATTERN_RIDGEDMULTI = "1b1v.pattern.ridgedmulti";

    /** Паттерн #field.
     * @category BASIC (O(1) noise sampling per block) */

    /** Паттерн #swirl.
     * @category BASIC [по аналогии с PATTERN_FIELD/PATTERN_SEAWEED, не верифицировано чтением кода] */

    /** Паттерн #clipboard.
     * @category BASIC (cache-hit O(1) через MasterClipboardCache; cache-miss — синхронный disk I/O
     * на Main Thread, редкий холодный старт) */

    /** Паттерн #fullcopy.
     * @category INTERMEDIATE (RAM — дедуп-кэш ConcurrentHashMap<Long> растёт с числом уникальных
     * блоков; для монолитов, не для тайлинга; governed worldedit.limit) */

    /** Паттерн #smooth.
     * @category ADVANCED (RAM — DensityMatrix до 4 массивов на объём региона, пик ~0.4GB на cap
     * 40_000_000 вокселей, см. ADL №8; параллельный multi-threaded blur) */

    /** Паттерн #stair.
     * @category BASIC (фиксированный O(27) обход соседей 3x3x3, без аллокаций сверх стека) */

    /** Паттерн #flowers.
     * @category BASIC (O(1) per-block noise sampling) */
    public static final String PATTERN_FLOWERS = "1b1v.pattern.flowers";

    /** Паттерн #grass.
     * @category INTERMEDIATE (precompute BitSet snapshot региона, как MASK_AROUND; cap не верифицирован) */
    public static final String PATTERN_GRASS = "1b1v.pattern.grass";

    /** Паттерн #seaweed.
     * @category BASIC [по аналогии с PATTERN_FLOWERS, не верифицировано чтением кода] */

    // --- MASKS ---

    /** Маска #around.
     * @category INTERMEDIATE (precompute BitSet региона, MAX_VOLUME=100_000_000 (~12MB), MAX_RADIUS=10) */
    public static final String MASK_AROUND = "1b1v.mask.around";

    /** Маска #road.
     * @category INTERMEDIATE [идентичная структура с MASK_AROUND (BitSet + RichParser)] */

    // --- MACROS ---

    /** Доступ к меню макросов, записи и удалению собственных макросов.
     * @category BASIC (Blast Radius: Self — gateway-право; без MACRO_CREATE_* не исполняет ни одной
     * команды, см. MacroManager.isCommandAllowed()) */

    /** Создание базовых макросов (whitelist безопасных команд, см. MacroManager.isCommandAllowed()).
     * @category INTERMEDIATE (наследует severity от худшего whitelisted процессора) */

    /** Создание продвинутых макросов (любые зарегистрированные команды).
     * @category ADVANCED (безусловный доступ через AliasRoutingService, в обход per-command проверок) */

    /** Префикс лимита количества макросов (напр. limit.count.5). */

    /** Префикс лимита длины макроса (напр. limit.length.3). */

    // --- PLAYER (Утилитарные команды для игроков) ---

    /** Команда /speed — кастомная замена Essentials speed с приоритетным перехватом
     * (есть право → выполняет Orion, нет права → событие не отменяется, фоллбэк в Essentials).
     * @category TODO */

    // --- Recolor System
    /** Recolor-инструмент: детерминированная shape-aware перекраска готовых построек
     * по маппингу OriginalBlockType -> NewBlockType (без GUI, debug-команда с ручными id).
     * @category MAPPING (Performance — построение маппинга происходит один раз на вызов
     * команды, стоимость пропорциональна числу уникальных пар, не размеру региона;
     * применение к региону — то же API, что у /set) */
    public static final String RECOLOR_USE = "1b1v.recolor.use";
}