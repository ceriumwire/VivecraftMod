/**
 * Copyright 2013 Mark Browning, StellaArtois
 * Licensed under the LGPL 3.0 or later (See LICENSE.md for details)
 */
package org.vivecraft.settings;

import org.vivecraft.DataHolder;
import org.vivecraft.extensions.OptionsExtension;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.vivecraft.settings.profile.ProfileManager;
import org.vivecraft.settings.profile.ProfileReader;
import org.vivecraft.settings.profile.ProfileWriter;
import org.vivecraft.gameplay.VRPlayer;
import org.vivecraft.gameplay.screenhandlers.KeyboardHandler;
import org.vivecraft.gui.PhysicalKeyboard;
import org.vivecraft.utils.LangHelper;
import org.vivecraft.utils.math.Angle;
import org.vivecraft.utils.math.Quaternion;
import org.vivecraft.utils.math.Vector3;

import java.awt.*;
import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class VRSettings
{
    public static final int VERSION = 2;
    public static final Logger logger = LogManager.getLogger();
    public static VRSettings inst;
    public JsonObject defaults = new JsonObject();
    public static final int UNKNOWN_VERSION = 0;
    public static final String DEGREE  = "\u00b0";

    public enum InertiaFactor implements OptionEnum<InertiaFactor> {
        NONE(1f / 0.01f),
        NORMAL(1f),
        LARGE(1f / 4f),
        MASSIVE(1f / 16f);

        private final float factor;

        InertiaFactor(float factor) {
            this.factor = factor;
        }

        public float getFactor() {
            return factor;
        }
    }

    public enum BowMode implements OptionEnum<BowMode> {
        OFF,
        VANILLA,
        ON
    }

    public enum RenderPointerElement implements OptionEnum<RenderPointerElement> {
        ALWAYS,
        WITH_HUD,
        NEVER
    }

    public enum ChatNotifications implements OptionEnum<ChatNotifications> {
        NONE,
        HAPTIC,
        SOUND,
        BOTH
    }

    public enum MirrorMode implements OptionEnum<MirrorMode> {
        OFF,
        CROPPED,
        SINGLE,
        DUAL,
        FIRST_PERSON,
        THIRD_PERSON,
        MIXED_REALITY
    }

    public enum HUDLock implements OptionEnum<HUDLock> {
        WRIST,
        HAND,
        HEAD
    }

    public enum FreeMove implements OptionEnum<FreeMove> {
        CONTROLLER,
        HMD,
        RUN_IN_PLACE,
        ROOM
    }

    public enum MenuWorld implements OptionEnum<MenuWorld> {
        BOTH,
        CUSTOM,
        OFFICIAL,
        NONE
    }

    public enum WeaponCollision implements OptionEnum<WeaponCollision> {
        OFF,
        ON,
        AUTO
    }

    public enum RightClickDelay implements OptionEnum<RightClickDelay> {
    	VANILLA,
    	SLOW,
    	SLOWER,
    	SLOWEST
    }
    
    @SettingField
    public int version = UNKNOWN_VERSION;

    @SettingField
    public String stereoProviderPluginID = "openvr";
    @SettingField
    public String badStereoProviderPluginID = "";
    public boolean storeDebugAim = false;
    @SettingField
    public int smoothRunTickCount = 20;
    @SettingField
    public boolean smoothTick = false;
    //Jrbudda's Options

    @SettingField(config = "QUICKCOMMAND", separate = true)
    public String[] vrQuickCommands = getQuickCommandsDefaults();
    @SettingField(config = "RADIAL", separate = true)
    public String[] vrRadialItems = getRadialItemsDefault();
    @SettingField(config = "RADIALALT", separate = true)
    public String[] vrRadialItemsAlt = getRadialItemsAltDefault();

    //Control
    @SettingField(VrOptions.REVERSE_HANDS)
    public boolean reverseHands = false;
    public boolean reverseShootingEye = false;
    @SettingField(value = VrOptions.WORLD_SCALE)
    public float worldScale = 1.0f;
    @SettingField(value = VrOptions.WORLD_ROTATION)
    public float worldRotation = 0f;
    public float worldRotationCached;
    @SettingField(value = VrOptions.WORLD_ROTATION_INCREMENT, config = "vrWorldRotationIncrement")
    public float worldRotationIncrement = 45f;
    @SettingField(VrOptions.X_SENSITIVITY)
    public float xSensitivity=1f;
    @SettingField(VrOptions.Y_SENSITIVITY)
    public float ySensitivity=1f;
    @SettingField(VrOptions.KEYHOLE)
    public float keyholeX=15;
    @SettingField
    public double headToHmdLength=0.10f;
    @SettingField
    public float autoCalibration=-1;
    @SettingField
    public float manualCalibration=-1;
    @SettingField
    public boolean alwaysSimulateKeyboard = false;
    @SettingField(VrOptions.BOW_MODE)
    public BowMode bowMode = BowMode.ON;
    @SettingField
    public String keyboardKeys =  "`1234567890-=qwertyuiop[]\\asdfghjkl;\':\"zxcvbnm,./?<>";
    @SettingField
    public String keyboardKeysShift ="~!@#$%^&*()_+QWERTYUIOP{}|ASDFGHJKL;\':\"ZXCVBNM,./?<>";
    @SettingField(VrOptions.HRTF_SELECTION)
    public int hrtfSelection = 0;
    @SettingField
    public boolean disableFun = false;
    @SettingField
    public boolean firstRun = true;
    @SettingField(VrOptions.RIGHT_CLICK_DELAY)
    public RightClickDelay rightclickDelay = RightClickDelay.VANILLA;
    @SettingField(VrOptions.THIRDPERSON_ITEMTRANSFORMS)
    public boolean thirdPersonItems = false;
    //

    //Locomotion
    @SettingField(VrOptions.INERTIA_FACTOR)
    public InertiaFactor inertiaFactor = InertiaFactor.NORMAL;
    @SettingField(VrOptions.WALK_UP_BLOCKS)
    public boolean walkUpBlocks = true;     // VIVE default to enable climbing
    @SettingField(VrOptions.SIMULATE_FALLING)
    public boolean simulateFalling = true;  // VIVE if HMD is over empty space, fall
    @SettingField(value = VrOptions.WEAPON_COLLISION, config = "weaponCollisionNew")
    public WeaponCollision weaponCollision = WeaponCollision.AUTO;  // VIVE weapon hand collides with blocks/enemies
    @SettingField(VrOptions.MOVEMENT_MULTIPLIER)
    public float movementSpeedMultiplier = 1.0f;   // VIVE - use full speed by default
    @SettingField(VrOptions.FREEMOVE_MODE)
    public FreeMove vrFreeMoveMode = FreeMove.CONTROLLER;
    @SettingField(value = VrOptions.LIMIT_TELEPORT, config = "limitedTeleport")
    public boolean vrLimitedSurvivalTeleport = true;

    @SettingField(value = VrOptions.TELEPORT_UP_LIMIT, config = "teleportLimitUp")
    public int vrTeleportUpLimit = 1;
    @SettingField(value = VrOptions.TELEPORT_DOWN_LIMIT, config = "teleportLimitDown")
    public int vrTeleportDownLimit = 4;
    @SettingField(value = VrOptions.TELEPORT_HORIZ_LIMIT, config = "teleportLimitHoriz")
    public int vrTeleportHorizLimit = 16;

    @SettingField(VrOptions.PLAY_MODE_SEATED)
    public boolean seated = false;
    @SettingField(value = VrOptions.SEATED_HMD, config = "seatedhmd")
    public boolean seatedUseHMD = false;
    @SettingField
    public float jumpThreshold=0.05f;
    @SettingField
    public float sneakThreshold=0.4f;
    @SettingField
    public float crawlThreshold = 0.82f;
    @SettingField(VrOptions.REALISTIC_JUMP)
    public boolean realisticJumpEnabled=true;
    @SettingField(VrOptions.REALISTIC_SNEAK)
    public boolean realisticSneakEnabled=true;
    @SettingField(VrOptions.REALISTIC_CLIMB)
    public boolean realisticClimbEnabled=true;
    @SettingField(VrOptions.REALISTIC_SWIM)
    public boolean realisticSwimEnabled=true;
    @SettingField(VrOptions.REALISTIC_ROW)
    public boolean realisticRowEnabled=true;
    @SettingField(VrOptions.BACKPACK_SWITCH)
    public boolean backpackSwitching = true;
    @SettingField(VrOptions.PHYSICAL_GUI)
    public boolean physicalGuiEnabled = false;
    @SettingField(VrOptions.WALK_MULTIPLIER)
    public float walkMultiplier=1;
    @SettingField(VrOptions.ALLOW_CRAWLING)
    public boolean allowCrawling = true;
    @SettingField(value = VrOptions.BCB_ON, config = "bcbOn")
    public boolean vrShowBlueCircleBuddy = true;
    @SettingField(VrOptions.VEHICLE_ROTATION)
    public boolean vehicleRotation = true;
    @SettingField(VrOptions.ANALOG_MOVEMENT)
    public boolean analogMovement = true;
    @SettingField(VrOptions.AUTO_SPRINT)
    public boolean autoSprint = true;
    @SettingField(VrOptions.AUTO_SPRINT_THRESHOLD)
    public float autoSprintThreshold = 0.9f;
    @SettingField
    public Vector3 originOffset = new Vector3(0.0F, 0.0F, 0.0F);
    @SettingField(VrOptions.ALLOW_STANDING_ORIGIN_OFFSET)
    public boolean allowStandingOriginOffset = false;
    @SettingField(VrOptions.SEATED_FREE_MOVE)
    public boolean seatedFreeMove = false;
    @SettingField(VrOptions.FORCE_STANDING_FREE_MOVE)
    public boolean forceStandingFreeMove = false;
    //

    //Rendering
    @SettingField(VrOptions.FSAA)
    public boolean useFsaa = false;   // default to off
    @SettingField(value = VrOptions.FOV_REDUCTION, config = "fovReduction")
    public boolean useFOVReduction = false;   // default to off
    @SettingField(VrOptions.FOV_REDUCTION_OFFSET)
    public float fovRedutioncOffset = 0.1f; // nice typo
    @SettingField(VrOptions.FOV_REDUCTION_MIN)
    public float fovReductionMin = 0.25f;
    @SettingField(value = VrOptions.STENCIL_ON, config = "stencilOn")
    public boolean vrUseStencil = true;
    @SettingField
    public boolean insideBlockSolidColor = false; //unused
    @SettingField(VrOptions.RENDER_SCALEFACTOR)
    public float renderScaleFactor = 1.0f;
    @SettingField(VrOptions.MIRROR_DISPLAY)
    public MirrorMode displayMirrorMode = MirrorMode.CROPPED;
    @SettingField(VrOptions.MIRROR_EYE)
    public boolean displayMirrorLeftEye = false;
    public boolean shouldRenderSelf=false;
    public boolean tmpRenderSelf;
    @SettingField(VrOptions.MENU_WORLD_SELECTION)
    public MenuWorld menuWorldSelection = MenuWorld.BOTH;
    //

    //Mixed Reality
    @SettingField(VrOptions.MIXED_REALITY_KEY_COLOR)
    public Color mixedRealityKeyColor = new Color(0, 0, 0);
    public float mixedRealityAspectRatio = 16F / 9F;
    @SettingField(VrOptions.MIXED_REALITY_RENDER_HANDS)
    public boolean mixedRealityRenderHands = false;
    @SettingField(VrOptions.MIXED_REALITY_UNITY_LIKE)
    public boolean mixedRealityUnityLike = true;
    @SettingField(VrOptions.MIXED_REALITY_UNDISTORTED)
    public boolean mixedRealityUndistorted = true;
    @SettingField(VrOptions.MIXED_REALITY_ALPHA_MASK)
    public boolean mixedRealityAlphaMask = false;
    @SettingField(VrOptions.MIXED_REALITY_FOV)
    public float mixedRealityFov = 40;
    @SettingField
    public float vrFixedCamposX = -1.0f;
    @SettingField
    public float vrFixedCamposY = 2.4f;
    @SettingField
    public float vrFixedCamposZ = 2.7f;
    @SettingField(config = "vrFixedCamrot", separate = true)
    public Quaternion vrFixedCamrotQuat =new Quaternion(.962f, .125f, .239f, .041f);
    @SettingField
    public float mrMovingCamOffsetX = 0;
    @SettingField
    public float mrMovingCamOffsetY = 0;
    @SettingField
    public float mrMovingCamOffsetZ = 0;
    @SettingField(config = "mrMovingCamOffsetRot", separate = true)
    public Quaternion mrMovingCamOffsetRotQuat = new Quaternion();
    @SettingField
    public Angle.Order externalCameraAngleOrder = Angle.Order.XZY;
    @SettingField(VrOptions.HANDHELD_CAMERA_FOV)
    public float handCameraFov = 70;
    @SettingField(VrOptions.HANDHELD_CAMERA_RENDER_SCALE)
    public float handCameraResScale = 1.0f;
    @SettingField(VrOptions.MIXED_REALITY_RENDER_CAMERA_MODEL)
    public boolean mixedRealityRenderCameraModel = true;
    //

    //HUD/GUI
    @SettingField(VrOptions.TOUCH_HOTBAR)
    public boolean vrTouchHotbar = true;
    @SettingField(value = VrOptions.HUD_SCALE, config = "headHudScale")
    public float hudScale = 1.0f;
    @SettingField(VrOptions.HUD_DISTANCE)
    public float hudDistance = 1.25f;
    @SettingField
    public float hudPitchOffset = -2f;
    @SettingField
    public float hudYawOffset = 0.0f;
    public boolean floatInventory = true; //false not working yet, have to account for rotation and tilt in MCOpenVR>processGui()
    @SettingField(VrOptions.MENU_ALWAYS_FOLLOW_FACE)
    public boolean menuAlwaysFollowFace;
    @SettingField(VrOptions.HUD_LOCK_TO)
    public HUDLock vrHudLockMode = HUDLock.WRIST;
    @SettingField(VrOptions.HUD_OCCLUSION)
    public boolean hudOcclusion = true;
    @SettingField(VrOptions.CROSSHAIR_SCALE)
    public float crosshairScale = 1.0f;
    @SettingField(VrOptions.CROSSHAIR_SCALES_WITH_DISTANCE)
    public boolean crosshairScalesWithDistance = false;
    @SettingField(VrOptions.RENDER_CROSSHAIR_MODE)
    public RenderPointerElement renderInGameCrosshairMode = RenderPointerElement.ALWAYS;
    @SettingField(VrOptions.RENDER_BLOCK_OUTLINE_MODE)
    public RenderPointerElement renderBlockOutlineMode = RenderPointerElement.ALWAYS;
    @SettingField(VrOptions.HUD_OPACITY)
    public float hudOpacity = 1f;
    @SettingField(VrOptions.RENDER_MENU_BACKGROUND)
    public boolean menuBackground = false;
    @SettingField(VrOptions.MENU_CROSSHAIR_SCALE)
    public float   menuCrosshairScale = 1f;
    @SettingField(VrOptions.CROSSHAIR_OCCLUSION)
    public boolean useCrosshairOcclusion = true;
    @SettingField(VrOptions.SEATED_HUD_XHAIR)
    public boolean seatedHudAltMode = true;
    @SettingField(VrOptions.AUTO_OPEN_KEYBOARD)
    public boolean autoOpenKeyboard = false;
    @SettingField
    public int forceHardwareDetection = 0; // 0 = off, 1 = vive, 2 = oculus
    @SettingField(VrOptions.RADIAL_MODE_HOLD)
    public boolean radialModeHold = true;
    @SettingField(VrOptions.PHYSICAL_KEYBOARD)
    public boolean physicalKeyboard = true;
    @SettingField(VrOptions.PHYSICAL_KEYBOARD_SCALE)
    public float physicalKeyboardScale = 1.0f;
    @SettingField(VrOptions.PHYSICAL_KEYBOARD_THEME)
    public PhysicalKeyboard.KeyboardTheme physicalKeyboardTheme = PhysicalKeyboard.KeyboardTheme.DEFAULT;
    @SettingField(VrOptions.ALLOW_ADVANCED_BINDINGS)
    public boolean allowAdvancedBindings = false;
    @SettingField(VrOptions.CHAT_NOTIFICATIONS)
    public ChatNotifications chatNotifications = ChatNotifications.NONE; // 0 = off, 1 = haptic, 2 = sound, 3 = both
    @SettingField(VrOptions.CHAT_NOTIFICATION_SOUND)
    public String chatNotificationSound = "block.note_block.bell";
    @SettingField(VrOptions.GUI_APPEAR_OVER_BLOCK)
    public boolean guiAppearOverBlock = true;

    /**
     * This isn't actually used, it's only a dummy field to save the value from vanilla Options.
     */
    @SettingField(VrOptions.HUD_HIDE) @Deprecated
    public boolean hideGUI;
    /**
     * This isn't actually used, it's only a dummy field to set the value in vanilla Options.
     */
    @SettingField(VrOptions.MONO_FOV) @Deprecated
    public float monoFOV;
    //

    public ServerOverrides overrides = new ServerOverrides();

    private Map<VrOptions, Triple<Field, String, Boolean>> fieldEnumMap = new EnumMap<>(VrOptions.class);
    private Map<String, Triple<Field, VrOptions, Boolean>> fieldConfigMap = new HashMap<>();

    // This map is only here to preserve old settings, not intended for general use
    private Map<String, String> preservedSettingMap;

    private Minecraft mc;

    public VRSettings( Minecraft minecraft, File dataDir )
    {
        // Need to do this in the instance because array sizes aren't known until instantiation
        initializeFieldInfo();

        // Assumes GameSettings (and hence optifine's settings) have been read first

        mc = minecraft;
        inst = this;

        // Store our class defaults to a member variable for later use
        storeDefaults();

        // Legacy config files. Note that in general these files will be by-passed
        // by the Profile handling in ProfileManager. loadOptions and saveOptions ill
        // be redirected to the profile manager using ProfileReader and ProfileWriter
        // respectively.

        // Load settings from the file
        this.loadOptions();
    }

    private void initializeFieldInfo() {
        try {
            for (Field field : VRSettings.class.getFields()) {
                SettingField ann = field.getAnnotation(SettingField.class);
                if (ann == null) continue;

                String config = ann.config().isEmpty() ? field.getName() : ann.config();
                if (ann.value() != VrOptions.DUMMY) {
                    if (fieldEnumMap.containsKey(ann.value()))
                        throw new RuntimeException("duplicate enum in setting field: " + field.getName());
                    fieldEnumMap.put(ann.value(), Triple.of(field, config, ann.separate()));
                }

                Triple<Field, VrOptions, Boolean> configEntry = Triple.of(field, ann.value(), ann.separate());
                if (ann.separate() && field.getType().isArray()) {
                    int len = Array.getLength(field.get(this));
                    IntStream.range(0, len).forEach(i -> fieldConfigMap.put(config + "_" + i, configEntry));
                } else if (ann.separate() && Quaternion.class.isAssignableFrom(field.getType())) {
                    Stream.of('W', 'X', 'Y', 'Z').forEach(suffix -> fieldConfigMap.put(config + suffix, configEntry));
                } else if (ann.separate() && Vector3.class.isAssignableFrom(field.getType())) {
                    Stream.of('X', 'Y', 'Z').forEach(suffix -> fieldConfigMap.put(config + suffix, configEntry));
                } else {
                    fieldConfigMap.put(config, configEntry);
                }
            }
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Object loadOption(String name, String value, Object currentValue, VrOptions option, Class<?> type, boolean separate) throws ReflectiveOperationException {
        // First try to convert the option from a legacy value
        Object obj = option.convertOption(value);
        // If that got nothing, try the custom handler
        if (obj == null) obj = option.loadOption(value);
        if (obj != null) return obj;

        // Generic handlers
        if (type == String.class) {
            return value;
        } else if (type == Boolean.TYPE) {
            return value.equals("true");
        } else if (type == Integer.TYPE) {
            return Integer.parseInt(value);
        } else if (type == Long.TYPE) {
            return Long.parseLong(value);
        } else if (type == Float.TYPE) {
            return Float.parseFloat(value);
        } else if (type == Double.TYPE) {
            return Double.parseDouble(value);
        } else if (type.isEnum()) {
            Method m = type.getMethod("valueOf", String.class);
            return m.invoke(null, value);
        } else if (Quaternion.class.isAssignableFrom(type)) {
            Quaternion quat = ((Quaternion)currentValue).copy();
            if (separate) {
                float f = Float.parseFloat(value);
                switch (name.charAt(name.length() - 1)) {
                    case 'W' -> quat.w = f;
                    case 'X' -> quat.x = f;
                    case 'Y' -> quat.y = f;
                    case 'Z' -> quat.z = f;
                }
            } else {
                String[] split = value.split(",");
                quat.w = Float.parseFloat(split[0]);
                quat.x = Float.parseFloat(split[1]);
                quat.y = Float.parseFloat(split[2]);
                quat.z = Float.parseFloat(split[3]);
            }
            return quat;
        } else if (Vector3.class.isAssignableFrom(type)) {
            Vector3 vec = ((Vector3)currentValue).copy();
            if (separate) {
                float f = Float.parseFloat(value);
                switch (name.charAt(name.length() - 1)) {
                    case 'X' -> vec.x = f;
                    case 'Y' -> vec.y = f;
                    case 'Z' -> vec.z = f;
                }
            } else {
                String[] split = value.split(",");
                vec.x = Float.parseFloat(split[0]);
                vec.y = Float.parseFloat(split[1]);
                vec.z = Float.parseFloat(split[2]);
            }
            return vec;
        }

        // If we get here, the value wasn't interpreted
        logger.warn("Don't know how to load VR option " + name + " with type " + type.getSimpleName());
        return null;
    }

    private String saveOption(String name, Object obj, VrOptions option, Class<?> type, boolean separate) {
        // Try the custom handler first
        String value = option.saveOption(obj);
        if (value != null) return value;

        // Generic handlers
        if (type == String.class) {
            return (String)obj;
        } else if (type == Boolean.TYPE || type == Integer.TYPE || type == Long.TYPE || type == Float.TYPE || type == Double.TYPE) {
            return obj.toString();
        } else if (type.isEnum()) {
            return ((Enum<?>)obj).name();
        } else if (Quaternion.class.isAssignableFrom(type)) {
            Quaternion quat = (Quaternion)obj;
            if (separate) {
                return Float.toString(switch (name.charAt(name.length() - 1)) {
                    case 'W' -> quat.w;
                    case 'X' -> quat.x;
                    case 'Y' -> quat.y;
                    case 'Z' -> quat.z;
                    default -> 0; // shouldn't happen
                });
            } else {
                return quat.w + "," + quat.x + "," + quat.y + "," + quat.z;
            }
        } else if (Vector3.class.isAssignableFrom(type)) {
            Vector3 vec = (Vector3)obj;
            if (separate) {
                return Float.toString(switch (name.charAt(name.length() - 1)) {
                    case 'X' -> vec.x;
                    case 'Y' -> vec.y;
                    case 'Z' -> vec.z;
                    default -> 0; // shouldn't happen
                });
            } else {
                return vec.x + "," + vec.y + "," + vec.z;
            }
        }

        // If we get here, the object wasn't interpreted
        logger.warn("Don't know how to save VR option " + name + " with type " + type.getSimpleName());
        return null;
    }

    private Object loadDefault(String name, String value, VrOptions option, Class<?> type, boolean separate, Map<String, String> profileSet) throws ReflectiveOperationException {
        if (value == null) value = profileSet.get(name);

        // Try the custom handler first
        Object obj = option.loadOption(value);
        if (obj != null) return obj;

        // Generic handlers
        if (type == String.class) {
            return value;
        } else if (type == Boolean.TYPE) {
            return value.equals("true");
        } else if (type == Integer.TYPE) {
            return Integer.parseInt(value);
        } else if (type == Long.TYPE) {
            return Long.parseLong(value);
        } else if (type == Float.TYPE) {
            return Float.parseFloat(value);
        } else if (type == Double.TYPE) {
            return Double.parseDouble(value);
        } else if (type.isEnum()) {
            Method m = type.getMethod("valueOf", String.class);
            return m.invoke(null, value);
        } else if (Quaternion.class.isAssignableFrom(type)) {
            Quaternion quat = new Quaternion();
            if (separate) {
                Stream.of('W', 'X', 'Y', 'Z').forEach(suffix -> {
                    String str = profileSet.get(name + suffix);
                    float f = Float.parseFloat(str);
                    switch (suffix) {
                        case 'W' -> quat.w = f;
                        case 'X' -> quat.x = f;
                        case 'Y' -> quat.y = f;
                        case 'Z' -> quat.z = f;
                    }
                });
            } else {
                String[] split = value.split(",");
                quat.w = Float.parseFloat(split[0]);
                quat.x = Float.parseFloat(split[1]);
                quat.y = Float.parseFloat(split[2]);
                quat.z = Float.parseFloat(split[3]);
            }
            return quat;
        } else if (Vector3.class.isAssignableFrom(type)) {
            Vector3 vec = new Vector3();
            if (separate) {
                Stream.of('X', 'Y', 'Z').forEach(suffix -> {
                    String str = profileSet.get(name + suffix);
                    float f = Float.parseFloat(str);
                    switch (suffix) {
                        case 'X' -> vec.x = f;
                        case 'Y' -> vec.y = f;
                        case 'Z' -> vec.z = f;
                    }
                });
            } else {
                String[] split = value.split(",");
                vec.x = Float.parseFloat(split[0]);
                vec.y = Float.parseFloat(split[1]);
                vec.z = Float.parseFloat(split[2]);
            }
            return vec;
        }

        // If we get here, the value wasn't interpreted
        logger.warn("Don't know how to load default VR option " + name + " with type " + type.getSimpleName());
        return null;
    }

    public void loadDefault(VrOptions option) {
        if (this.defaults == null) return; // how

        try {
            var mapping = fieldEnumMap.get(option);
            if (mapping == null) return;
            Field field = mapping.getLeft();
            Class<?> type = field.getType();
            String name = mapping.getMiddle();

            Map<String, String> profileSet = ProfileManager.getProfileSet(this.defaults, ProfileManager.PROFILE_SET_VR);

            if (type.isArray()) {
                Object arr = field.get(this);
                int len = Array.getLength(arr);
                if (mapping.getRight()) {
                    for (int i = 0; i < len; i++) {
                        Object obj = Objects.requireNonNull(loadDefault(name + "_" + i, null, option, type.getComponentType(), false, profileSet));
                        Array.set(arr, i, obj);
                    }
                } else {
                    String str = profileSet.get(name);
                    String[] split = str.split(";", -1); // Avoid conflicting with other comma-delimited types
                    for (int i = 0; i < len; i++) {
                        Object obj = Objects.requireNonNull(loadDefault(name, split[i], option, type.getComponentType(), false, profileSet));
                        Array.set(arr, i, obj);
                    }
                }
            } else {
                Object obj = Objects.requireNonNull(loadDefault(name, null, option, type, mapping.getRight(), profileSet));
                field.set(this, obj);
            }
        } catch (Exception ex) {
            logger.warn("Failed to load default VR option: " + option);
            ex.printStackTrace();
        }
    }

    public void loadOptions()
    {
        loadOptions(null);
    }

    public void loadDefaults()
    {
        loadOptions(this.defaults);
    }

    public void loadOptions(JsonObject theProfiles)
    {
        // Load Minecrift options
        try
        {
            ProfileReader optionsVRReader = new ProfileReader(ProfileManager.PROFILE_SET_VR, theProfiles);

            String var2 = "";

            while ((var2 = optionsVRReader.readLine()) != null)
            {
                try
                {
                    String[] optionTokens = var2.split(":", 2);
                    String name = optionTokens[0];
                    String value = optionTokens.length > 1 ? optionTokens[1] : "";

                    var mapping = fieldConfigMap.get(name);
                    if (mapping == null) continue;

                    Field field = mapping.getLeft();
                    Class<?> type = field.getType();
                    Object currentValue = field.get(this);
                    if (type.isArray()) {
                        if (mapping.getRight()) {
                            int index = Integer.parseInt(name.substring(name.lastIndexOf('_') + 1));
                            Object obj = Objects.requireNonNull(loadOption(name.substring(0, name.lastIndexOf('_')), value, Array.get(currentValue, index), mapping.getMiddle(), type.getComponentType(), false));
                            Array.set(currentValue, index, obj);
                        } else {
                            int len = Array.getLength(currentValue);
                            String[] split = value.split(";", -1); // Avoid conflicting with other comma-delimited types
                            for (int i = 0; i < len; i++) {
                                Object obj = Objects.requireNonNull(loadOption(name, split[i], Array.get(currentValue, i), mapping.getMiddle(), type.getComponentType(), false));
                                Array.set(currentValue, i, obj);
                            }
                        }
                    } else {
                        Object obj = Objects.requireNonNull(loadOption(name, value, currentValue, mapping.getMiddle(), type, mapping.getRight()));
                        field.set(this, obj);
                    }
                }
                catch (Exception var7)
                {
                    logger.warn("Skipping bad VR option: " + var2);
                    var7.printStackTrace();
                }
            }

            preservedSettingMap = optionsVRReader.getData();
            optionsVRReader.close();
        }
        catch (Exception var8)
        {
            logger.warn("Failed to load VR options!");
            var8.printStackTrace();
        }
    }

    public void saveOptions()
    {
        saveOptions(null); // Use null for current profile
    }

    private void storeDefaults()
    {
        saveOptions(this.defaults);
    }

    private void saveOptions(JsonObject theProfiles)
    {
        // Save Minecrift settings
        try
        {
            ProfileWriter var5 = new ProfileWriter(ProfileManager.PROFILE_SET_VR, theProfiles);
            if (preservedSettingMap != null)
                var5.setData(preservedSettingMap);

            for (var entry : fieldConfigMap.entrySet()) {
                String name = entry.getKey();
                var mapping = entry.getValue();
                Field field = mapping.getLeft();
                Class<?> type = field.getType();
                Object obj = field.get(this);

                try {
                    if (type.isArray()) {
                        if (mapping.getRight()) {
                            int index = Integer.parseInt(name.substring(name.lastIndexOf('_') + 1));
                            String value = Objects.requireNonNull(saveOption(name.substring(0, name.lastIndexOf('_')), Array.get(obj, index), mapping.getMiddle(), type.getComponentType(), mapping.getRight()));
                            var5.println(name + ":" + value);
                        } else {
                            StringJoiner joiner = new StringJoiner(";");
                            int len = Array.getLength(obj);
                            for (int i = 0; i < len; i++) {
                                String value = Objects.requireNonNull(saveOption(name, Array.get(obj, i), mapping.getMiddle(), type.getComponentType(), mapping.getRight()));
                                joiner.add(value);
                            }
                            var5.println(name + ":" + joiner.toString());
                        }
                    } else {
                        String value = Objects.requireNonNull(saveOption(name, obj, mapping.getMiddle(), type, mapping.getRight()));
                        var5.println(name + ":" + value);
                    }
                } catch (Exception ex) {
                    logger.warn("Failed to save VR option: " + name);
                    ex.printStackTrace();
                }
            }

            var5.close();
        }
        catch (Exception var3)
        {
            logger.warn("Failed to save VR options: " + var3.getMessage());
            var3.printStackTrace();
        }
    }

    public void resetSettings()
    {
        // Get the Minecrift defaults
        loadDefaults();
    }

    public String getButtonDisplayString( VrOptions par1EnumOptions )
    {
        String var2 = I18n.get("vivecraft.options." + par1EnumOptions.name());

        String var3 = var2 + ": ";
        String var4 = var3;
        String var5;

        try {
            var mapping = fieldEnumMap.get(par1EnumOptions);
            if (mapping == null) return var2;
            Field field = mapping.getLeft();
            Class<?> type = field.getType();

            Object obj = field.get(this);
            if (overrides.hasSetting(par1EnumOptions))
                obj = this.overrides.getSetting(par1EnumOptions).getValue();

            String str = par1EnumOptions.getDisplayString(var4, obj);
            if (str != null) {
                return str;
            } else if (type == Boolean.TYPE) {
                var langKeys = par1EnumOptions.getBooleanLangKeys();
                return (boolean)obj ? var4 + I18n.get(langKeys.getLeft()) : var4 + I18n.get(langKeys.getRight());
            } else if (type == Float.TYPE || type == Double.TYPE) {
                if (par1EnumOptions.getDecimalPlaces() < 0) {
                    return var4 + Math.round(((Number)obj).floatValue() * 100) + "%";
                } else {
                    return var4 + String.format("%." + par1EnumOptions.getDecimalPlaces() + "f", ((Number)obj).floatValue());
                }
            } else if (OptionEnum.class.isAssignableFrom(type)) {
                return var4 + I18n.get(((OptionEnum<?>)obj).getLangKey());
            } else {
                return var4 + obj.toString();
            }
        } catch (Exception ex) {
            System.out.println("Failed to get VR option display string: " + par1EnumOptions);
            ex.printStackTrace();
        }

        return var2;
    }

    public float getOptionFloatValue(VrOptions par1EnumOptions)
    {
        try {
            var mapping = fieldEnumMap.get(par1EnumOptions);
            if (mapping == null) return 0;
            Field field = mapping.getLeft();

            float value = ((Number)field.get(this)).floatValue();
            if (overrides.hasSetting(par1EnumOptions))
                value = overrides.getSetting(par1EnumOptions).getFloat();

            return Objects.requireNonNullElse(par1EnumOptions.getOptionFloatValue(value), value);
        } catch (Exception ex) {
            System.out.println("Failed to get VR option float value: " + par1EnumOptions);
            ex.printStackTrace();
        }

        return 0.0f;
    }
    /**
     * For non-float options. Toggles the option on/off, or cycles through the list i.e. render distances.
     */
    public void setOptionValue(VrOptions par1EnumOptions)
    {
        try {
            var mapping = fieldEnumMap.get(par1EnumOptions);
            if (mapping == null) return;
            Field field = mapping.getLeft();
            Class<?> type = field.getType();

            Object obj = par1EnumOptions.setOptionValue(field.get(this));
            if (obj != null) {
                field.set(this, obj);
            } else if (type == Boolean.TYPE) {
                field.set(this, !(boolean)field.get(this));
            } else if (OptionEnum.class.isAssignableFrom(type)) {
                field.set(this, ((OptionEnum<?>)field.get(this)).getNext());
            } else {
                logger.warn("Don't know how to set VR option " + mapping.getMiddle() + " with type " + type.getSimpleName());
                return;
            }

            par1EnumOptions.onOptionChange();
            this.saveOptions();
        } catch (Exception ex) {
            System.out.println("Failed to set VR option: " + par1EnumOptions);
            ex.printStackTrace();
        }
    }

    public void setOptionFloatValue(VrOptions par1EnumOptions, float par2)
    {
        try {
            var mapping = fieldEnumMap.get(par1EnumOptions);
            if (mapping == null) return;
            Field field = mapping.getLeft();
            Class<?> type = field.getType();

            float f = Objects.requireNonNullElse(par1EnumOptions.setOptionFloatValue(par2), par2);
            if (overrides.hasSetting(par1EnumOptions))
                f = Mth.clamp(f, overrides.getSetting(par1EnumOptions).getValueMin(), overrides.getSetting(par1EnumOptions).getValueMax());

            if (type == Integer.TYPE) {
                field.set(this, (int)f);
            } else if (type == Long.TYPE) {
                field.set(this, (long)f);
            } else {
                field.set(this, f);
            }

            par1EnumOptions.onOptionChange();
            this.saveOptions();
        } catch (Exception ex) {
            System.out.println("Failed to set VR option float value: " + par1EnumOptions);
            ex.printStackTrace();
        }
    }

    /**
     * Parses a string into a float.
     */
    private float parseFloat(String par1Str)
    {
        return par1Str.equals("true") ? 1.0F : (par1Str.equals("false") ? 0.0F : Float.parseFloat(par1Str));
    }

    public float getHeadTrackSensitivity()
    {
        //if (this.useQuaternions)
        return 1.0f;

        //return this.headTrackSensitivity;  // TODO: If head track sensitivity is working again... if
    }


    public static enum VrOptions
    {
        DUMMY(false, true), // Dummy
        HUD_SCALE(true, false, 0.35f, 2.5f, 0.01f, -1), // Head HUD Size
        HUD_DISTANCE(true, false, 0.25f, 5.0f, 0.01f, 2) { // Head HUD Distance
            @Override
            String getDisplayString(String prefix, Object value) {
                return prefix + String.format("%.2f", (float)value) + "m";
            }
        },
        HUD_LOCK_TO(false, true) { // HUD Orientation Lock
            @Override
            Object convertOption(String value) {
                // TODO: remove conversion in the future
                try {
                    int ord = Integer.parseInt(value);
                    return HUDLock.values()[3 - ord];
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
        },
        HUD_OPACITY(true, false, 0.15f, 1.0f, 0.05f, -1) { // HUD Opacity
            @Override
            String getDisplayString(String prefix, Object value) {
                if ((float)value > 0.99)
                    return prefix + I18n.get("vivecraft.options.opaque");
                return null;
            }
        },
        HUD_HIDE(false, true) { // Hide HUD (F1)
            @Override
            Object loadOption(String value) {
                Minecraft.getInstance().options.hideGui = value.equals("true");
                return false;
            }

            @Override
            String saveOption(Object value) {
                return Boolean.toString(Minecraft.getInstance().options.hideGui);
            }

            @Override
            String getDisplayString(String prefix, Object value) {
                return Minecraft.getInstance().options.hideGui ? prefix + LangHelper.getYes() : prefix + LangHelper.getNo();
            }

            @Override
            Object setOptionValue(Object value) {
                Minecraft.getInstance().options.hideGui = !Minecraft.getInstance().options.hideGui;
                return false;
            }
        },
        RENDER_MENU_BACKGROUND(false, true), // HUD/GUI Background
        HUD_OCCLUSION(false, true), // HUD Occlusion
        MENU_ALWAYS_FOLLOW_FACE(false, true, "vivecraft.options.always", "vivecraft.options.seated"), // Main Menu Follow
        CROSSHAIR_OCCLUSION(false, true), // Crosshair Occlusion
        CROSSHAIR_SCALE(true, false, 0.25f, 1.0f, 0.01f, -1), // Crosshair Size
        MENU_CROSSHAIR_SCALE(true, false, 0.25f, 2.5f, 0.05f, -1), // Menu Crosshair Size
        RENDER_CROSSHAIR_MODE(false, true) { // Show Crosshair
            @Override
            Object convertOption(String value) {
                // TODO: remove conversion in the future
                try {
                    int ord = Integer.parseInt(value);
                    return RenderPointerElement.values()[ord];
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
        },
        CHAT_NOTIFICATIONS(false, true) { // Chat Notifications
            @Override
            Object convertOption(String value) {
                // TODO: remove conversion in the future
                try {
                    int ord = Integer.parseInt(value);
                    return ChatNotifications.values()[ord];
                } catch (NumberFormatException ex) { // new method
                    return null;
                }
            }
        },
        CHAT_NOTIFICATION_SOUND(false, true) { // Notification Sound
            @Override
            String getDisplayString(String prefix, Object value) {
                try {
                    SoundEvent se = Registry.SOUND_EVENT.get(new ResourceLocation((String)value));
                    return I18n.get(se.getLocation().getPath());
                } catch (Exception e) {
                    return "error";
                }
            }

            @Override
            Object setOptionValue(Object value) {
                SoundEvent se = Registry.SOUND_EVENT.get(new ResourceLocation((String)value));
                int i = Registry.SOUND_EVENT.getId(se);
                if (++i >= Registry.SOUND_EVENT.keySet().size())
                    i = 0;
                return Registry.SOUND_EVENT.byId(i).getLocation().getPath();
            }
        },
        CROSSHAIR_SCALES_WITH_DISTANCE(false, true), // Crosshair Scaling
        RENDER_BLOCK_OUTLINE_MODE(false, true) { // Show Block Outline
            @Override
            Object convertOption(String value) {
                // TODO: remove conversion in the future
                try {
                    int ord = Integer.parseInt(value);
                    return RenderPointerElement.values()[ord];
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
        },
        AUTO_OPEN_KEYBOARD(false, true), // Always Open Keyboard
        RADIAL_MODE_HOLD(false, true, "vivecraft.options.hold", "vivecraft.options.press"), // Radial Menu Mode
        PHYSICAL_KEYBOARD(false, true, "vivecraft.options.keyboard.physical", "vivecraft.options.keyboard.pointer"), // Keyboard Type
        PHYSICAL_KEYBOARD_SCALE(true, false, 0.75f, 1.5f, 0.01f, -1) { // Keyboard Size
            @Override
            void onOptionChange() {
                KeyboardHandler.physicalKeyboard.setScale(DataHolder.getInstance().vrSettings.physicalKeyboardScale);
            }
        },
        PHYSICAL_KEYBOARD_THEME(false, false), // Keyboard Theme
        GUI_APPEAR_OVER_BLOCK(false, true), // Appear Over Block
        //HMD/render
        FSAA(false, true), // Lanczos Scaler
        MIRROR_DISPLAY(false, true) { // Desktop Mirror
            @Override
            Object convertOption(String value) {
                // TODO: remove conversion in the future
                try {
                    int ord = Integer.parseInt(value);
                    return switch (ord) {
                        case 10 -> MirrorMode.OFF; // MIRROR_OFF
                        case 11 -> MirrorMode.DUAL; // MIRROR_ON_DUAL
                        case 12 -> MirrorMode.SINGLE; // MIRROR_ON_SINGLE
                        case 16 -> MirrorMode.CROPPED; // MIRROR_ON_CROPPED
                        default -> MirrorMode.values()[ord - 9];
                    };
                } catch (NumberFormatException ex) {
                    return null;
                }
            }

            @Override
            void onOptionChange() {
            	DataHolder.getInstance().vrRenderer.reinitFrameBuffers("Mirror Setting Changed");
            }

            @Override
            Object setOptionValue(Object value) {
                // TODO: remove this method after fixing mixed reality... again
                MirrorMode mode = ((MirrorMode)value).getNext();
                if (mode == MirrorMode.MIXED_REALITY)
                    mode = mode.getNext();
                return mode;
            }
        },
        MIRROR_EYE(false, true, "vivecraft.options.left", "vivecraft.options.right"), // Mirror Eye
        MIXED_REALITY_KEY_COLOR(false, false) { // Key Color
            private static final List<Pair<Color, String>> colors;
            static {
                colors = new ArrayList<>();
                colors.add(Pair.of(new Color(0, 0, 0), "vivecraft.options.color.black"));
                colors.add(Pair.of(new Color(255, 0, 0), "vivecraft.options.color.red"));
                colors.add(Pair.of(new Color(255, 255, 0), "vivecraft.options.color.yellow"));
                colors.add(Pair.of(new Color(0, 255, 0), "vivecraft.options.color.green"));
                colors.add(Pair.of(new Color(0, 255, 255), "vivecraft.options.color.cyan"));
                colors.add(Pair.of(new Color(0, 0, 255), "vivecraft.options.color.blue"));
                colors.add(Pair.of(new Color(255, 0, 255), "vivecraft.options.color.magenta"));
            }

            @Override
            String getDisplayString(String prefix, Object value) {
                Color color = (Color)value;
                var p = colors.stream().filter(c -> c.getLeft().equals(color)).findFirst().orElse(null);
                return p != null ? prefix + I18n.get(p.getRight()) : prefix + color.getRed() + " " + color.getGreen() + " " + color.getBlue();
            }

            @Override
            Object loadOption(String value) {
                String[] split = value.split(",");
                return new Color(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
            }

            @Override
            String saveOption(Object value) {
                Color color = (Color)value;
                return color.getRed() + "," + color.getGreen() + "," + color.getBlue();
            }

            @Override
            Object setOptionValue(Object value) {
                int index = IntStream.range(0, colors.size()).filter(i -> colors.get(i).getLeft().equals((Color)value)).findFirst().orElse(-1);
                return index == -1 || index == colors.size() - 1 ? colors.get(0).getLeft() : colors.get(index + 1).getLeft();
            }
        },
        MIXED_REALITY_RENDER_HANDS(false, true), // Show Hands
        MIXED_REALITY_UNITY_LIKE(false, true, "vivecraft.options.unity", "vivecraft.options.sidebyside") { // Layout
            @Override
            void onOptionChange() {
            	DataHolder.getInstance().vrRenderer.reinitFrameBuffers("MR Setting Changed");
            }
        },
        MIXED_REALITY_UNDISTORTED(false, true) { // Undistorted Pass
            @Override
            void onOptionChange() {
            	DataHolder.getInstance().vrRenderer.reinitFrameBuffers("MR Setting Changed");
            }
        },
        MIXED_REALITY_ALPHA_MASK(false, true) { // Alpha Mask
            @Override
            void onOptionChange() {
            	DataHolder.getInstance().vrRenderer.reinitFrameBuffers("MR Setting Changed");
            }
        },
        MIXED_REALITY_FOV(true, false, 0, 179, 1, 0) { // Third Person FOV
            @Override
            String getDisplayString(String prefix, Object value) {
                return prefix + String.format("%.0f" + DEGREE, (float)value);
            }
        },
        WALK_UP_BLOCKS(false, true), // Walk up blocks
        //Movement/aiming controls
        MOVEMENT_MULTIPLIER(true, false, 0.15f, 1.3f, 0.01f, 2), // Move. Speed Multiplier
        INERTIA_FACTOR(false, true) { // Player Inertia
            @Override
            Object convertOption(String value) {
                // TODO: remove conversion in the future
                try {
                    int ord = Integer.parseInt(value);
                    return InertiaFactor.values()[ord];
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
        },
        // VIVE START - new options
        SIMULATE_FALLING(false, true), // Simulate falling
        WEAPON_COLLISION(false, true) { // Weapon collision
            @Override
            Object convertOption(String value) {
                // TODO: remove conversion in the future
                try {
                    int ord = Integer.parseInt(value);
                    return WeaponCollision.values()[ord];
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
        },
        // VIVE END - new options
        //JRBUDDA VIVE
        ALLOW_CRAWLING(false, true), // Roomscale Crawling
        LIMIT_TELEPORT(false, true), // Limit in Survival
        REVERSE_HANDS(false, true), // Reverse Hands
        STENCIL_ON(false, true), // Use Eye Stencil
        BCB_ON(false, true), // Show Body Position
        WORLD_SCALE(true, false, 0, 29, 1, 2) { // World Scale
            @Override
            String getDisplayString(String prefix, Object value) {
                return prefix + String.format("%.2f", (float)value) + "x";
            }

            @Override
            Float getOptionFloatValue(float value) {
                if (value == 0.1f) return 0f;
                if (value == 0.25f) return 1f;
                if (value >= 0.5f && value <= 2.0f) return (value / 0.1f) - 3f;
                if (value == 3) return 18f;
                if (value == 4) return 19f;
                if (value == 6) return 20f;
                if (value == 8) return 21f;
                if (value == 10) return 22f;
                if (value == 12) return 23f;
                if (value == 16) return 24f;
                if (value == 20) return 25f;
                if (value == 30) return 26f;
                if (value == 50) return 27f;
                if (value == 75) return 28f;
                if (value == 100) return 29f;
                return 7f;
            }

            @Override
            Float setOptionFloatValue(float value) {
                if (value == 0) return 0.1f;
                else if (value == 1) return 0.25f;
                else if (value >= 2 && value <= 17) return value * 0.1f + 0.3f;
                else if (value == 18) return 3f;
                else if (value == 19) return 4f;
                else if (value == 20) return 6f;
                else if (value == 21) return 8f;
                else if (value == 22) return 10f;
                else if (value == 23) return 12f;
                else if (value == 24) return 16f;
                else if (value == 25) return 20f;
                else if (value == 26) return 30f;
                else if (value == 27) return 50f;
                else if (value == 28) return 75f;
                else if (value == 29) return 100f;
                else return 1f;
            }

            @Override
            void onOptionChange() {
                DataHolder.getInstance().vrPlayer.roomScaleMovementDelay = 2;
                DataHolder.getInstance().vrPlayer.snapRoomOriginToPlayerEntity(Minecraft.getInstance().player, false, true);
                VRPlayer.get().preTick();
            }
        },
        WORLD_ROTATION(true, false, 0, 360, 30, 0) { // World Rotation
            @Override
            String getDisplayString(String prefix, Object value) {
                return prefix + String.format("%.0f" + DEGREE, (float)value);
            }

            @Override
            Float setOptionFloatValue(float value) {
                return null;
            }
        },
        WORLD_ROTATION_INCREMENT(true, false, -1, 4, 1, 0) { // Rotation Increment
            @Override
            String getDisplayString(String prefix, Object value) {
                if ((float)value == 0)
                    return prefix + ("vivecraft.options.smooth");
                return prefix + String.format("%.0f" + DEGREE, (float)value);
            }

            @Override
            Float getOptionFloatValue(float value) {
                if (value == 0) return -1f;
                if (value == 10f) return 0f;
                if (value == 36f) return 1f;
                if (value == 45f) return 2f;
                if (value == 90f) return 3f;
                if (value == 180f) return 4f;
                return 2f;
            }

            @Override
            Float setOptionFloatValue(float value) {
                if (value == -1f) return 0f;
                if (value == 0f) return 10f;
                if (value == 1f) return 36f;
                if (value == 2f) return 45f;
                if (value == 3f) return 90f;
                if (value == 4f) return 180f;
                return 45f;
            }

            @Override
            void onOptionChange() {
            	DataHolder.getInstance().vrSettings.worldRotation = 0;
            }
        },
        TOUCH_HOTBAR(false, true), // Touch Hotbar Enabled
        PLAY_MODE_SEATED(false, true, "vivecraft.options.seated", "vivecraft.options.standing"), // Play Mode
        RENDER_SCALEFACTOR(true, false, 0.1f, 9f, 0.1f, 0) { // Resolution
            @Override
            String getDisplayString(String prefix, Object value) {
                RenderTarget eye0 = DataHolder.getInstance().vrRenderer.framebufferEye0;
                return prefix + Math.round((float)value * 100) + "% (" + (int)Math.ceil(eye0.viewWidth * Math.sqrt((float)value)) + "x" + (int)Math.ceil(eye0.viewHeight * Math.sqrt((float)value)) + ")";
            }
        },
        MONO_FOV(true, false, 0, 179, 1, 0) { // Undistorted FOV
            @Override
            String getDisplayString(String prefix, Object value) {
                return prefix + String.format("%.0f" + DEGREE, Minecraft.getInstance().options.fov);
            }

            @Override
            Float getOptionFloatValue(float value) {
                return (float)Minecraft.getInstance().options.fov;
            }

            @Override
            Float setOptionFloatValue(float value) {
                Minecraft.getInstance().options.fov = value;
                return 0f;
            }
        },
        HANDHELD_CAMERA_FOV(true, false, 0, 179, 1, 0) { // Camera FOV
            @Override
            String getDisplayString(String prefix, Object value) {
                return prefix + String.format("%.0f" + DEGREE, (float)value);
            }
        },
        HANDHELD_CAMERA_RENDER_SCALE(true, false, 0.5f, 3.0f, 0.25f, 0) { // Camera Resolution
            @Override
            String getDisplayString(String prefix, Object value) {
//                if (Config.isShaders()) { //optifine
//                    RenderTarget camfb = Minecraft.getInstance().vrRenderer.cameraFramebuffer;
//                    return prefix + camfb.viewWidth + "x" + camfb.viewHeight;
//                } else {
                    return prefix + Math.round(1920 * (float)value) + "x" + Math.round(1080 * (float)value);
//                }
            }
        },
        MIXED_REALITY_RENDER_CAMERA_MODEL(false, true, LangHelper.YES_KEY, LangHelper.NO_KEY), // Show Camera Model
        //END JRBUDDA
        REALISTIC_JUMP(false, true), // Roomscale Jumping
        REALISTIC_SNEAK(false, true), // Roomscale Sneaking
        PHYSICAL_GUI(false, true) { // Physical GUIs
            @Override
            Object loadOption(String value) {
                // TODO: fix physical GUI... someday
                return false;
            }
        },
        REALISTIC_CLIMB(false, true), // Roomscale Climbing
        REALISTIC_SWIM(false, true), // Roomscale Swimming
        REALISTIC_ROW(false, true), // Roomscale Rowing
        WALK_MULTIPLIER(true, false, 1f, 10f, 0.1f, 1), // Walking Multiplier
        FREEMOVE_MODE(false, true) { // Free Move Type
            @Override
            Object convertOption(String value) {
                // TODO: remove conversion in the future
                try {
                    int ord = Integer.parseInt(value);
                    return switch (ord) {
                        case 4 -> FreeMove.CONTROLLER; // legacy FREEMOVE_JOYPAD
                        case 5 -> FreeMove.ROOM; // FREEMOVE_ROOM
                        default -> FreeMove.values()[ord - 1];
                    };
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
        },
        VEHICLE_ROTATION(false, true), // Vehicle Rotation
        //SEATED
        RESET_ORIGIN(false, true), // Reset Origin
        X_SENSITIVITY(true, false, 0.1f, 5f, 0.01f, 2), // Rotation Speed
        Y_SENSITIVITY(true, false, 0.1f, 5f, 0.01f, 2), // Y Sensitivity
        KEYHOLE(true, false, 0f, 40f, 5f, 0) { // Keyhole
            @Override
            String getDisplayString(String prefix, Object value) {
                return prefix + String.format("%.0f" + DEGREE, (float)value);
            }
        },
        FOV_REDUCTION(false, true), // FOV Comfort Reduction
        FOV_REDUCTION_MIN(true, false, 0.1f, 0.7f, 0.05f, 2), // FOV Reduction Size
        FOV_REDUCTION_OFFSET(true, false, 0.0f, 0.3f, 0.01f, 2), // FOV Reduction Offset
        // OTher buttons
        SEATED_HMD(false, true, "vivecraft.options.hmd", "vivecraft.options.crosshair"), // Forward Direction
        SEATED_HUD_XHAIR(false, true, "vivecraft.options.crosshair", "vivecraft.options.hmd"), // HUD Follows
        BACKPACK_SWITCH(false, true), // Backpack Switching
        ANALOG_MOVEMENT(false, true), // Analog Movement
        AUTO_SPRINT(false, true), // Auto-sprint
        AUTO_SPRINT_THRESHOLD(true, false, 0.5f, 1f, 0.01f, 2), // Auto-sprint Threshold
        THIRDPERSON_ITEMTRANSFORMS(false, true), // 3rd person items
        BOW_MODE(false, true) { // Roomscale Bow Mode
            @Override
            Object convertOption(String value) {
                // TODO: remove conversion in the future
                try {
                    int ord = Integer.parseInt(value);
                    return BowMode.values()[ord];
                } catch (NumberFormatException ex) { // new method
                    return null;
                }
            }
        },
        TELEPORT_DOWN_LIMIT(true, false, 0, 16, 1, 0) { // Down Limit
            @Override
            String getDisplayString(String prefix, Object value) {
                return (int)value > 0 ? prefix + LangHelper.get("vivecraft.options.teleportlimit", (int)value) : prefix + "OFF";
            }
        },
        TELEPORT_UP_LIMIT(true, false, 0, 4, 1, 0) { // Up Limit
            @Override
            String getDisplayString(String prefix, Object value) {
                return (int)value > 0 ? prefix + LangHelper.get("vivecraft.options.teleportlimit", (int)value) : prefix + "OFF";
            }
        },
        TELEPORT_HORIZ_LIMIT(true, false, 0, 32, 1, 0) { // Distance Limit
            @Override
            String getDisplayString(String prefix, Object value) {
                return (int)value > 0 ? prefix + LangHelper.get("vivecraft.options.teleportlimit", (int)value) : prefix + "OFF";
            }
        },
        ALLOW_STANDING_ORIGIN_OFFSET(false, true, LangHelper.YES_KEY, LangHelper.NO_KEY), // Allow Origin Offset
        SEATED_FREE_MOVE(false, true, "vivecraft.options.freemove", "vivecraft.options.teleport"), // Movement Type
        FORCE_STANDING_FREE_MOVE(false, true, LangHelper.YES_KEY, LangHelper.NO_KEY), // Force Free Move
        ALLOW_ADVANCED_BINDINGS(false, true, LangHelper.YES_KEY, LangHelper.NO_KEY), // Show Advanced Bindings
        MENU_WORLD_SELECTION(false, false) { // Worlds
            @Override
            Object convertOption(String value) {
                // TODO: remove conversion in the future
                try {
                    int ord = Integer.parseInt(value);
                    return MenuWorld.values()[ord];
                } catch (NumberFormatException ex) { // new method
                    return null;
                }
            }
        },
        HRTF_SELECTION(false, false) { // HRTF
            @Override
            String getDisplayString(String prefix, Object value) {
                int i = (int)value;
                if (i == -1)
                    return prefix + I18n.get("options.off");
                else if (i == 0)
                    return prefix + I18n.get("vivecraft.options.default");
                else if (i <= DataHolder.hrtfList.size())
                    return prefix + DataHolder.hrtfList.get(i - 1);
                return prefix;
            }

            @Override
            Object setOptionValue(Object value) {
                int i = (int)value;
                if (++i > DataHolder.hrtfList.size())
                    i = -1;
                return i;
            }

            @Override
            void onOptionChange() {
                // Reload the sound engine to get the new HRTF
                SoundEngine eng = Minecraft.getInstance().getSoundManager().soundEngine;
                eng.reload();
            }
        },
        RELOAD_EXTERNAL_CAMERA(false, false) { // Reload External Camera
            @Override
            String getDisplayString(String prefix, Object value) {
                return I18n.get("vivecraft.options." + name());
            }
        },
        RIGHT_CLICK_DELAY(false, false); // Right Click Repeat
//        ANISOTROPIC_FILTERING("options.anisotropicFiltering", true, false, 1.0F, 16.0F, 0.0F)
//                {
//                    private static final String __OBFID = "CL_00000654";
//                    protected float snapToStep(float p_148264_1_)
//                    {
//                        return (float) MathHelper.roundUpToPowerOfTwo((int) p_148264_1_);
//                    }
//                },

        private final boolean enumFloat;
        private final boolean enumBoolean;
        private final float valueStep;
        private final float valueMin;
        private final float valueMax;
        private final int decimalPlaces;
        private final Pair<String, String> booleanLangKeys;

        public static VrOptions getEnumOptions(int par0)
        {
            VrOptions[] aoptions = values();
            int j = aoptions.length;

            for (int k = 0; k < j; ++k)
            {
                VrOptions options = aoptions[k];

                if (options.returnEnumOrdinal() == par0)
                {
                    return options;
                }
            }

            return null;
        }

        VrOptions(boolean isfloat, boolean isbool)
        {
            this(isfloat, isbool, 0.0F, 1.0F, 0.0F, 0);
        }

        VrOptions(boolean isfloat, boolean isbool, String trueLangKey, String falseKangKey)
        {
            this(isfloat, isbool, 0.0F, 1.0F, 0.0F, 0, trueLangKey, falseKangKey);
        }

        /**
         *
         * @param isfloat
         * @param isbool
         * @param min
         * @param max
         * @param step
         * @param decimalPlaces number of decimal places for float value, negative to display as percentage
         */
        VrOptions(boolean isfloat, boolean isbool, float min, float max, float step, int decimalPlaces) {
            this(isfloat, isbool, min, max, step, decimalPlaces, LangHelper.ON_KEY, LangHelper.OFF_KEY);
        }

        VrOptions(boolean isfloat, boolean isboolean, float min, float max, float step, int decimalPlaces, String trueLangKey, String falseKangKey)
        {
            this.enumFloat = isfloat;
            this.enumBoolean = isboolean;
            this.valueMin = min;
            this.valueMax = max;
            this.valueStep = step;
            this.decimalPlaces = decimalPlaces;
            this.booleanLangKeys = Pair.of(trueLangKey, falseKangKey);
        }

        Object convertOption(String value) {
            return null;
        }

        Object loadOption(String value) {
            return null;
        }

        String saveOption(Object value) {
            return null;
        }

        String getDisplayString(String prefix, Object value) {
            return null;
        }

        Object setOptionValue(Object value) {
            return null;
        }

        Float getOptionFloatValue(float value) {
            return null;
        }

        Float setOptionFloatValue(float value) {
            return null;
        }

        void onOptionChange() {}

        public boolean getEnumFloat()
        {
            return this.enumFloat;
        }

        public boolean getEnumBoolean()
        {
            return this.enumBoolean;
        }

        public int returnEnumOrdinal()
        {
            return this.ordinal();
        }

        public float getValueMax()
        {
            return this.valueMax;
        }

        public float getValueMin()
        {
            return this.valueMin;
        }

        public int getDecimalPlaces() {
            return decimalPlaces;
        }

        public Pair<String, String> getBooleanLangKeys() {
            return booleanLangKeys;
        }

        protected float snapToStep(float p_148264_1_)
        {
            if (this.valueStep > 0.0F)
            {
                p_148264_1_ = this.valueStep * (float)Math.round(p_148264_1_ / this.valueStep);
            }

            return p_148264_1_;
        }

        public double normalizeValue(float value)
        {
            return Mth.clamp((this.snapToStep(value) - this.valueMin) / (this.valueMax - this.valueMin), 0.0D, 1.0D);
        }

        public double denormalizeValue(float value)
        {
            return this.snapToStep((float) (this.valueMin + (this.valueMax - this.valueMin) * Mth.clamp(value, 0.0D, 1.0D)));
        }
    }

    public static synchronized void initSettings( Minecraft mc, File dataDir )
    {
        ProfileManager.init(dataDir);
        mc.options = new Options( mc, dataDir );
        // mc.gameSettings.saveOptions();
        DataHolder.getInstance().vrSettings = new VRSettings( mc, dataDir );
        DataHolder.getInstance().vrSettings.saveOptions();
    }

    public static synchronized void loadAll( Minecraft mc )
    {
        mc.options.load();
        DataHolder.getInstance().vrSettings.loadOptions();
    }

    public static synchronized void saveAll( Minecraft mc )
    {
        mc.options.save();
        DataHolder.getInstance().vrSettings.saveOptions();
    }

    public static synchronized void resetAll( Minecraft mc )
    {
    	((OptionsExtension) mc.options).resetSettings();
        DataHolder.getInstance().vrSettings.resetSettings();
    }

    public static synchronized String getCurrentProfile()
    {
        return ProfileManager.getCurrentProfileName();
    }

    public static synchronized boolean profileExists(String profile)
    {
        return ProfileManager.profileExists(profile);
    }

    public static synchronized SortedSet<String> getProfileList()
    {
        return ProfileManager.getProfileList();
    }

    public static synchronized boolean setCurrentProfile(String profile)
    {
        StringBuilder error = new StringBuilder();
        return setCurrentProfile(profile, error);
    }

    public static synchronized boolean setCurrentProfile(String profile, StringBuilder error)
    {
        boolean result = true;
        Minecraft mc = Minecraft.getInstance();

        // Save settings in current profile
        VRSettings.saveAll(mc);

        // Set the new profile
        result = ProfileManager.setCurrentProfile(profile, error);

        if (result) {
            // Load new profile
            VRSettings.loadAll(mc);
        }

        return result;
    }

    public static synchronized boolean createProfile(String profile, boolean useDefaults, StringBuilder error)
    {
        boolean result = true;
        Minecraft mc = Minecraft.getInstance();
        String originalProfile = VRSettings.getCurrentProfile();

        // Save settings in original profile
        VRSettings.saveAll(mc);

        // Create the new profile
        if (!ProfileManager.createProfile(profile, error))
            return false;

        // Set the new profile
        ProfileManager.setCurrentProfile(profile, error);

        // Save existing settings as new profile...

        if (useDefaults) {
            // ...unless set to use defaults
            VRSettings.resetAll(mc);
        }

        // Save new profile settings to file
        VRSettings.saveAll(mc);

        // Select the original profile
        ProfileManager.setCurrentProfile(originalProfile, error);
        VRSettings.loadAll(mc);

        return result;
    }

    public static synchronized boolean deleteProfile(String profile)
    {
        StringBuilder error = new StringBuilder();
        return deleteProfile(profile, error);
    }

    public static synchronized boolean deleteProfile(String profile, StringBuilder error)
    {
        Minecraft mc = Minecraft.getInstance();

        // Save settings in current profile
        VRSettings.saveAll(mc);

        // Nuke the profile data
        if (!ProfileManager.deleteProfile(profile, error))
            return false;

        // Load settings in case the selected profile has changed
        VRSettings.loadAll(mc);

        return true;
    }

    public static synchronized boolean duplicateProfile(String originalProfile, String newProfile, StringBuilder error)
    {
        Minecraft mc = Minecraft.getInstance();

        // Save settings in current profile
        VRSettings.saveAll(mc);

        // Duplicate the profile data
        if (!ProfileManager.duplicateProfile(originalProfile, newProfile, error))
            return false;

        return true;
    }

    public static synchronized boolean renameProfile(String originalProfile, String newProfile, StringBuilder error)
    {
        Minecraft mc = Minecraft.getInstance();

        // Save settings in current profile
        VRSettings.saveAll(mc);

        // Rename the profile
        if (!ProfileManager.renameProfile(originalProfile, newProfile, error))
            return false;

        return true;
    }

    public String[] getQuickCommandsDefaults(){

        String[] out = new String[12];
        out[0] = "/gamemode survival";
        out[1] = "/gamemode creative";
        out[2] = "/help";
        out[3] = "/home";
        out[4] = "/sethome";
        out[5] = "/spawn";
        out[6] = "hi!";
        out[7] = "bye!";
        out[8] = "follow me!";
        out[9] = "take this!";
        out[10] = "thank you!";
        out[11] = "praise the sun!";

        return out;

    }

    public String[] getRadialItemsDefault(){
        String[] out = new String[8];
        out[0] = "key.drop";
        out[1] = "key.chat";
        out[2] = "vivecraft.key.rotateRight";
        out[3] = "key.pickItem";
        out[4] = "vivecraft.key.toggleHandheldCam";
        out[5] = "vivecraft.key.togglePlayerList";
        out[6] = "vivecraft.key.rotateLeft";
        out[7] = "vivecraft.key.quickTorch";

        return out;
    }

    public String[] getRadialItemsAltDefault(){
        String[] out = new String[8];
        out[0] = "";
        out[1] = "";
        out[2] = "";
        out[3] = "";
        out[4] = "";
        out[5] = "";
        out[6] = "";
        out[7] = "";

        return out;
    }

    public double normalizeValue(float optionFloatValue) {
        // TODO Auto-generated method stub
        return 0;
    }

    public class ServerOverrides {
        private Map<VrOptions, Setting> optionMap = new EnumMap<>(VrOptions.class);
        private Map<String, Setting> networkNameMap = new HashMap<>();

        private ServerOverrides() {
            registerSetting(VrOptions.LIMIT_TELEPORT, "limitedTeleport", () -> vrLimitedSurvivalTeleport);
            registerSetting(VrOptions.TELEPORT_UP_LIMIT, "teleportLimitUp", () -> vrTeleportUpLimit);
            registerSetting(VrOptions.TELEPORT_DOWN_LIMIT, "teleportLimitDown", () -> vrTeleportDownLimit);
            registerSetting(VrOptions.TELEPORT_HORIZ_LIMIT, "teleportLimitHoriz", () -> vrTeleportHorizLimit);
            registerSetting(VrOptions.WORLD_SCALE, "worldScale", () -> worldScale);
        }

        private void registerSetting(VrOptions option, String networkName, Supplier<Object> originalValue) {
            Setting setting = new Setting(option, networkName, originalValue);
            optionMap.put(option, setting);
            networkNameMap.put(networkName, setting);
        }

        public void resetAll() {
            for (Setting setting : optionMap.values()) {
                setting.valueSet = false;
                setting.valueMinSet = false;
                setting.valueMaxSet = false;
            }
        }

        public boolean hasSetting(VrOptions option) {
            return optionMap.containsKey(option);
        }

        public boolean hasSetting(String networkName) {
            return networkNameMap.containsKey(networkName);
        }

        public Setting getSetting(VrOptions option) {
            Setting setting = optionMap.get(option);
            if (setting == null)
                throw new IllegalArgumentException("setting not registered: " + option);

            return setting;
        }

        public Setting getSetting(String networkName) {
            Setting setting = networkNameMap.get(networkName);
            if (setting == null)
                throw new IllegalArgumentException("setting not registered: " + networkName);

            return setting;
        }

        public class Setting {
            private final VrOptions option;
            private final String networkName;
            private final Supplier<Object> originalValue;

            private boolean valueSet;
            private Object value;

            // For float options
            private boolean valueMinSet, valueMaxSet;
            private float valueMin, valueMax;

            public Setting(VrOptions option, String networkName, Supplier<Object> originalValue) {
                this.option = option;
                this.networkName = networkName;
                this.originalValue = originalValue;
            }

            private void checkFloat() {
                if (!option.enumFloat)
                    throw new IllegalArgumentException("not a float option: " + option);
            }

            public boolean isFloat() {
                return option.enumFloat;
            }

            public Object getOriginalValue() {
                return originalValue.get();
            }

            public boolean isValueOverridden() {
                return valueSet;
            }

            public Object getValue() {
                Object val;
                if (valueSet)
                    val = value;
                else
                    val = originalValue.get();

                if (val instanceof Integer)
                    val = Mth.clamp(((Number)val).intValue(), (int)getValueMin(), (int)getValueMax());
                else if (val instanceof Float)
                    val = Mth.clamp(((Number)val).floatValue(), getValueMin(), getValueMax());

                return val;
            }

            public boolean getBoolean() {
                Object val = getValue();
                return val instanceof Boolean ? (boolean)val : false;
            }

            public int getInt() {
                Object val = getValue();
                return val instanceof Number ? ((Number)val).intValue() : 0;
            }

            public float getFloat() {
                Object val = getValue();
                return val instanceof Number ? ((Number)val).floatValue() : 0;
            }

            public String getString() {
                Object val = getValue();
                return val instanceof String ? val.toString() : "";
            }

            public void setValue(Object value) {
                this.value = value;
                valueSet = true;
            }

            public void resetValue() {
                valueSet = false;
            }

            public boolean isValueMinOverridden() {
                checkFloat();
                return valueMinSet;
            }

            public float getValueMin() {
                checkFloat();
                if (valueMinSet)
                    return valueMin;
                else
                    return Float.MIN_VALUE;
            }

            public void setValueMin(float valueMin) {
                checkFloat();
                this.valueMin = valueMin;
                valueMinSet = true;
            }

            public void resetValueMin() {
                checkFloat();
                valueMinSet = false;
            }

            public boolean isValueMaxOverridden() {
                checkFloat();
                return valueMaxSet;
            }

            public float getValueMax() {
                checkFloat();
                if (valueMaxSet)
                    return valueMax;
                else
                    return Float.MAX_VALUE;
            }

            public void setValueMax(float valueMax) {
                checkFloat();
                this.valueMax = valueMax;
                valueMaxSet = true;
            }

            public void resetValueMax() {
                checkFloat();
                valueMaxSet = false;
            }
        }
    }
}
