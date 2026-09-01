package dev.nitrium.config;

/**
 * User-facing and governor tuning values. Persisted to {@code config/nitrium.json}.
 */
public final class NitriumConfig {
	public int targetFps = 60;
	public boolean enableGpuProfiling = true;
	public boolean enableDebugOverlay = false;

	/** Interval between slow-loop structural shader evaluations (seconds). */
	public int slowLoopIntervalSeconds = 7;

	/** FPS must stay below this before the slow loop considers downgrades. */
	public int slowLoopFpsThreshold = 30;

	/** Seconds below threshold before triggering a structural shader change. */
	public int slowLoopStabilitySeconds = 5;

	/** Adaptive render scale bounds. */
	public float renderScaleMin = 0.5f;
	public float renderScaleMax = 1.0f;

	// --- Dynamic resolution scaling (GPU-time driven) ---

	/**
	 * Enable GPU-time-driven render-scale recommendations. Advisory on vanilla (no runtime
	 * resolution scaling); consumed by the AZDO/Iris framebuffer bridge when present.
	 */
	public boolean enableDynamicResolution = false;

	/** Fraction of the per-frame budget the GPU pass should target before scale is reduced. */
	public float renderScaleGpuBudgetFraction = 0.9f;

	/** Relative error band around the GPU budget within which render scale is held steady. */
	public float renderScaleDeadband = 0.08f;

	/** Maximum change in linear render scale per fast-loop step (limits resolution pumping). */
	public float renderScaleMaxStep = 0.05f;

	// --- Adaptive render distance (FPS driven) ---

	/**
	 * Automatically grow/shrink Minecraft render distance to hold the target FPS. Off by default.
	 * Nitrium never applies render-distance changes at runtime because each one forces a full chunk
	 * reload; when enabled, values are advisory for the debug overlay only.
	 */
	public boolean enableAdaptiveRenderDistance = false;

	/** Lower bound for adaptive render distance (chunks). */
	public int adaptiveMinRenderDistanceChunks = 8;

	/** Upper bound for adaptive render distance (chunks); trimmed further on low-VRAM GPUs. */
	public int adaptiveMaxRenderDistanceChunks = 32;

	/** Grow render distance only when average FPS stays above targetFps × this fraction. */
	public float renderDistanceGrowFpsFraction = 1.15f;

	/** Shrink render distance when average FPS falls below targetFps × this fraction. */
	public float renderDistanceShrinkFpsFraction = 0.85f;

	/** Settle time after each render-distance change before the next adjustment. */
	public int renderDistanceCooldownSeconds = 3;

	// --- Structural shader quality governor (slow loop) ---

	/** Step shader quality down when average FPS is below targetFps × this fraction. */
	public float shaderDowngradeFpsFraction = 0.80f;

	/** Step shader quality up when average FPS is above targetFps × this fraction. */
	public float shaderUpgradeFpsFraction = 1.25f;

	/** Sustained seconds below the downgrade threshold before stepping quality down. */
	public int shaderDowngradeStabilitySeconds = 5;

	/** Sustained seconds above the upgrade threshold before stepping quality up. */
	public int shaderUpgradeStabilitySeconds = 15;

	/** Cooldown after any shader quality change to prevent recompile thrashing. */
	public int shaderTransitionCooldownSeconds = 20;

	/** Rolling average window for bottleneck classification (frames). */
	public int metricsWindowFrames = 120;

	/** Fraction of target frame budget attributed to GPU before classifying as GPU-bound. */
	public float gpuBoundBudgetFraction = 0.55f;

	/** Fraction of target frame budget attributed to CPU tick before classifying as CPU-bound. */
	public float cpuBoundBudgetFraction = 0.55f;

	// --- Extreme render distance / streaming pipeline ---

	/**
	 * Client-side binary section disk cache (Bobby-style). Off by default: it snapshots a full 16³
	 * section and queues a disk write on every block change, but nothing reads the cache back for
	 * rendering yet — so it only adds per-block CPU and disk churn. Re-enable once the cache feeds
	 * extended-distance rendering.
	 */
	public boolean enableSectionDiskCache = false;

	/** Client render distance extension beyond server view distance (blocks). 0 = disabled. */
	public int extendedRenderDistanceBlocks = 32;

	/** Max concurrent async section reads from disk. */
	public int maxConcurrentCacheReads = 8;

	/** Max concurrent off-thread mesh build tasks. */
	public int maxConcurrentMeshTasks = 4;

	/** Megabyte budget hint for the unified geometry buffer pool. */
	public int geometryBufferBudgetMb = 256;

	// --- GPU-aware culling pipeline ---

	public boolean enableCullingPipeline = true;

	/** Hi-Z GPU occlusion for terrain sections (requires OpenGL 4.3+ compute). */
	public boolean enableHiZOcclusion = true;

	/** Light-frustum intersection culling for Iris shadow passes. */
	public boolean enableShadowFrustumCulling = true;

	/** GPU occlusion queries + velocity-expanded proxies for entities. */
	public boolean enableEntityOcclusion = true;

	/** Distance-adaptive foliage alpha-test policy for distant leaves. */
	public boolean enableFoliageOptimization = true;

	/** Blocks beyond which foliage uses opaque alpha-test path. */
	public int foliageOpaqueDistanceBlocks = 32;

	/** Entity velocity lookahead in ticks for temporal proxy expansion. */
	public int entityVelocityLookaheadTicks = 3;

	/** Max shadow cast distance used by light-frustum culling (blocks). */
	public int shadowCullDistanceBlocks = 128;

	/** Low-res proxy buffer size for entity occlusion queries (pixels). */
	public int entityProxyBufferSize = 64;

	// --- Entity optimization (tick throttling, spatial grid, GPU instancing) ---

	public boolean enableEntityOptimization = true;

	/** Spatial hash bucket size in blocks (4 = 4×4×4 sub-buckets). */
	public int spatialBucketSize = 4;

	public int tier0DistanceBlocks = 16;
	public int tier1DistanceBlocks = 48;
	public int tier2DistanceBlocks = 96;

	/** Ticks without movement before enclosure auto-dormant check (100 = 5s). */
	public int enclosureIdleTicks = 100;

	/** Worker threads for parallel region ticking. */
	public int parallelTickWorkerThreads = 4;

	public boolean enableAnimationThrottling = true;

	/**
	 * GPU-instanced entity proxy draw. Off by default: it currently renders placeholder proxy
	 * geometry (not final entity models) on top of vanilla rendering, and its whole per-frame
	 * extraction (entity list + SIMD cull + transforms) feeds only that path, so enabling it costs
	 * CPU and GPU for no visual gain. The real, working entity distance culling is separate and
	 * unaffected.
	 */
	public boolean enableGpuEntityInstancing = false;

	// --- Entity render distance culling (client, FPS) ---

	/** Skip rendering entities beyond an adaptive distance cap (never culls players/mounts). */
	public boolean enableEntityDistanceCulling = true;

	/** Hard cap on entity render distance (blocks); also clamped to the terrain render distance. */
	public int maxEntityRenderDistanceBlocks = 96;

	/** Shorter render distance for cheap decorative entities (items, XP orbs). */
	public int itemEntityRenderDistanceBlocks = 48;

	/** Nearby radius (blocks) within which entities are never distance-culled. */
	public int entityCullNearRadiusBlocks = 16;

	/** Multiply the entity render distance by this when the frame is GPU/CPU stressed. */
	public float entityCullStressFactor = 0.65f;

	/**
	 * Multiply the entity render distance by this when an Iris shader pack is active. Shader packs
	 * redraw entities in the shadow pass, so a shorter entity distance is a large shadow-pass win.
	 */
	public float shaderEntityCullFactor = 0.7f;

	// --- Native core (JNI / SIMD / off-heap) ---

	public boolean enableNativeCore = true;

	/** OpenGL 4.4 AZDO: persistent mapped buffers + MDI (Iris-compatible path). */
	public boolean enableAzdoBackend = true;

	/** Future: Vulkan 1.3 backend (breaks Iris — experimental). */
	public boolean enableVulkanBackend = false;

	/** Future: hardware RT BVH acceleration for path-traced shaders. */
	public boolean enableHardwareRayTracing = false;

	// --- World generation optimization (server) ---

	/** SIMD noise, coarse-grid density, lock-free task graph for chunk generation. */
	public boolean enableWorldgenOptimization = true;

	/** Coarse-grid step for X/Z density sampling (blocks). */
	public int worldgenCoarseStepXz = 4;

	/** Coarse-grid step for Y density sampling (blocks). */
	public int worldgenCoarseStepY = 8;

	/** Density gradient threshold — full-resolution resample above this. */
	public float worldgenHighGradientThreshold = 0.35f;

	/** Worker threads for lock-free worldgen task graph. */
	public int worldgenWorkerThreads = 4;

	/** Enable AVX2 native 3D noise when native core is available. */
	public boolean enableSimdNoise = true;

	// --- Mod compatibility ---

	/**
	 * When true, Nitrium automatically tunes worker counts and memory budgets from detected CPU/GPU
	 * hardware (NVIDIA, AMD, Intel, core count, VRAM). User config values act as ceilings.
	 */
	public boolean enableHardwareAutoTune = true;

	/**
	 * When true, Nitrium automatically defers features that overlap with known performance mods
	 * (Lithium, Starlight, C2ME, Clumps, Krypton, FerriteCore, etc.).
	 */
	public boolean enableCompatibilityAutoDisable = true;

	/**
	 * Force-enable all Nitrium features even when a conflicting mod is present.
	 * Use only for testing — may cause double-patching or tick conflicts.
	 */
	public boolean compatibilityForceEnableAll = false;

	// --- Dynamic lighting engine ---

	public boolean enableLightingEngine = true;

	/** Worker threads for async light propagation DAG. */
	public int lightWorkerThreads = 2;

	/** Delay block-light updates by 1 tick and merge bounding boxes. */
	public boolean enableLightUpdateBatching = true;

	// --- Redstone & block entity optimization ---

	public boolean enableRedstoneOptimization = true;

	/** Skip tick for empty hoppers/furnaces until neighbor inventory changes. */
	public boolean enableBlockEntitySleep = true;

	/** Topological BFS redstone wire solver (intended to replace the vanilla evaluator). */
	public boolean enableTopologicalRedstone = true;

	// --- Item / XP pooling ---

	public boolean enableItemXpPooling = true;

	/** Merge radius in blocks for XP orbs and dropped items. */
	public int itemMergeRadiusBlocks = 3;

	/** Beyond this distance, client renders items as GPU billboards (no entity tick). */
	public int distantItemBillboardBlocks = 16;

	// --- Network pipeline ---

	public boolean enableNetworkPipeline = true;

	/** Use native SIMD compression for packet encode/decode. */
	public boolean enableNativePacketCompression = true;

	/** Off-thread packet compression worker threads. */
	public int networkWorkerThreads = 2;

	// --- Memory layout (SoA / bitfield palettes) ---

	public boolean enableMemoryLayoutOptimization = true;

	/** Deduplicate registry holders and compress block state flags into bitfields. */
	public boolean enableBlockStateBitfields = true;

	// --- Client rendering pipeline (OIT, particles, GUI, audio) ---

	/** Weighted blended order-independent transparency for water/glass/foliage. */
	public boolean enableOitTranslucency = true;

	/** GPU compute particle simulation with SSBO + indirect draw. */
	public boolean enableGpuParticles = true;

	/** Max particles tracked in GPU SSBO. */
	public int maxGpuParticles = 65536;

	/** SDF font atlas + cached HUD framebuffer layer. */
	public boolean enableGuiSdfCache = true;

	/** Async spatial audio occlusion via voxel grid (client). */
	public boolean enableAsyncAudioOcclusion = true;

	/** Audio occlusion worker threads. */
	public int audioWorkerThreads = 2;

	/** Voxel grid cell size for audio occlusion (blocks). */
	public int audioVoxelSizeBlocks = 4;

	// --- Async chunk storage (server) ---

	/**
	 * Ring-buffer async chunk saver. Off by default: the drain currently writes each queued chunk to
	 * a write-only file under {@code .nitrium/chunk-queue/} that nothing reads back or deletes, so it
	 * only grows disk usage (and re-serializes every chunk) with no benefit. Re-enable once the drain
	 * writes real region data.
	 */
	public boolean enableAsyncChunkStorage = false;

	/** Ring buffer capacity for pending chunk writes (MB). */
	public int chunkSaveRingBufferMb = 64;

	/** Max concurrent native chunk write operations. */
	public int maxConcurrentChunkWrites = 4;

	/**
	 * Region-file write compression for worlds this game saves (singleplayer / integrated server).
	 * Minecraft records the codec per chunk and can read any of them, so changing this is fully
	 * backwards/forwards compatible — no world conversion, no corruption risk.
	 * <ul>
	 *   <li>{@code default} — leave Minecraft's choice (Deflate/zlib): smallest files.</li>
	 *   <li>{@code lz4} — much faster compress/decompress at the cost of ~1.5–2× larger region files;
	 *       smoother chunk I/O when moving fast or at high render distance.</li>
	 *   <li>{@code none} — no compression: fastest I/O, largest files.</li>
	 *   <li>{@code deflate} — force zlib explicitly.</li>
	 * </ul>
	 * Dedicated servers should use {@code region-file-compression} in server.properties instead.
	 */
	public String regionFileCompression = "default";
}
