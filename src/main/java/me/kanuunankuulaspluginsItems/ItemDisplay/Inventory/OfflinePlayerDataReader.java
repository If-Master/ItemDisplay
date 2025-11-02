package me.kanuunankuulaspluginsItems.ItemDisplay.Inventory;

import me.kanuunankuulaspluginsItems.ItemDisplay.ItemDisplay;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


public class OfflinePlayerDataReader {
    private static boolean DEBUG = true;

    private static class ReflectionCache {
        static Class<?> nbtCompoundClass;
        static Class<?> nbtListClass;
        static Class<?> nbtStringClass;
        static Class<?> nbtBaseClass;
        static Class<?> nbtIoClass;
        static Class<?> nbtAccounterClass;

        static Method compoundGetList;
        static Method compoundPut;
        static Method compoundPutString;
        static Method compoundGetString;
        static Method compoundPutByte;
        static Method compoundGetByte;
        static Method compoundPutShort;
        static Method compoundGetShort;
        static Method compoundGetCompound;
        static Method compoundPutInt;
        static Method compoundGetInt;

        static Method listSize;
        static Method listGet;
        static Method listAdd;

        static Method stringCreate;

        static Method nbtRead;
        static Method nbtWrite;
        static Object unlimitedAccounter;

        static boolean usesOptional = false;
        static boolean isModernVersion = false;
        static String serverVersion;

        static {
            try {
                initializeReflection();
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize NBT reflection cache", e);
            }
        }

        private static void initializeReflection() throws Exception {
            serverVersion = getServerVersionInternal();

            nbtCompoundClass = findClass(
                    "net.minecraft.nbt.NbtCompound",
                    "net.minecraft.nbt.NBTTagCompound",
                    "net.minecraft.server." + serverVersion + ".NBTTagCompound"
            );

            nbtListClass = findClass(
                    "net.minecraft.nbt.NbtList",
                    "net.minecraft.nbt.NBTTagList",
                    "net.minecraft.server." + serverVersion + ".NBTTagList"
            );

            nbtStringClass = findClass(
                    "net.minecraft.nbt.NbtString",
                    "net.minecraft.nbt.NBTTagString",
                    "net.minecraft.server." + serverVersion + ".NBTTagString"
            );

            nbtBaseClass = findClass(
                    "net.minecraft.nbt.Tag",
                    "net.minecraft.nbt.NBTBase",
                    "net.minecraft.server." + serverVersion + ".NBTBase"
            );

            nbtIoClass = findClass(
                    "net.minecraft.nbt.NbtIo",
                    "net.minecraft.nbt.NBTCompressedStreamTools",
                    "net.minecraft.server." + serverVersion + ".NBTCompressedStreamTools"
            );

            nbtAccounterClass = findClass(
                    "net.minecraft.nbt.NbtAccounter",
                    "net.minecraft.nbt.NbtAccounter"
            );

            if (nbtAccounterClass != null) {
                unlimitedAccounter = getUnlimitedAccounter();
            }

            compoundGetList = findMethod(nbtCompoundClass, new String[]{"getList", "get"}, String.class, int.class);
            if (compoundGetList == null) {
                compoundGetList = findMethod(nbtCompoundClass, new String[]{"getList"}, String.class);
                usesOptional = true;
            }

            compoundPut = findMethod(nbtCompoundClass, new String[]{"put", "set"}, String.class, nbtBaseClass);
            compoundPutString = findMethod(nbtCompoundClass, new String[]{"putString", "setString"}, String.class, String.class);
            compoundGetString = findMethod(nbtCompoundClass, new String[]{"getString"}, String.class);
            compoundPutByte = findMethod(nbtCompoundClass, new String[]{"putByte", "setByte"}, String.class, byte.class);
            compoundGetByte = findMethod(nbtCompoundClass, new String[]{"getByte"}, String.class);
            compoundPutShort = findMethod(nbtCompoundClass, new String[]{"putShort", "setShort"}, String.class, short.class);
            compoundGetShort = findMethod(nbtCompoundClass, new String[]{"getShort"}, String.class);
            compoundGetCompound = findMethod(nbtCompoundClass, new String[]{"getCompound"}, String.class);
            compoundPutInt = findMethod(nbtCompoundClass, new String[]{"putInt", "setInt"}, String.class, int.class);
            compoundGetInt = findMethod(nbtCompoundClass, new String[]{"getInt"}, String.class);

            listSize = findMethod(nbtListClass, new String[]{"size", "d"});
            listGet = findMethod(nbtListClass, new String[]{"get", "a", "getCompound"}, int.class);
            listAdd = findMethod(nbtListClass, new String[]{"add", "a"}, nbtBaseClass);
            if (listAdd == null) {
                listAdd = findMethod(nbtListClass, new String[]{"add"}, int.class, nbtBaseClass);
            }

            stringCreate = findMethod(nbtStringClass, new String[]{"of", "a"}, String.class);

            initializeIOMethodsOptimized();

            isModernVersion = detectModernVersion();
        }
    }

    private static boolean detectModernVersion() {
        try {
            Class.forName("net.minecraft.core.component.DataComponents");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    private static void initializeIOMethodsOptimized() throws Exception {
        try {
            ReflectionCache.nbtRead = ReflectionCache.nbtIoClass.getMethod("readCompressed", java.nio.file.Path.class, ReflectionCache.nbtAccounterClass);
        } catch (NoSuchMethodException e1) {
            try {
                ReflectionCache.nbtRead = ReflectionCache.nbtIoClass.getMethod("readCompressed", InputStream.class, ReflectionCache.nbtAccounterClass);
            } catch (NoSuchMethodException e2) {
                try {
                    ReflectionCache.nbtRead = ReflectionCache.nbtIoClass.getMethod("a", java.nio.file.Path.class, ReflectionCache.nbtAccounterClass);
                } catch (NoSuchMethodException e3) {
                    ReflectionCache.nbtRead = ReflectionCache.nbtIoClass.getMethod("a", DataInputStream.class);
                }
            }
        }

        try {
            ReflectionCache.nbtWrite = ReflectionCache.nbtIoClass.getMethod("writeCompressed", ReflectionCache.nbtCompoundClass, java.nio.file.Path.class);
        } catch (NoSuchMethodException e1) {
            try {
                ReflectionCache.nbtWrite = ReflectionCache.nbtIoClass.getMethod("writeCompressed", ReflectionCache.nbtCompoundClass, OutputStream.class);
            } catch (NoSuchMethodException e2) {
                try {
                    ReflectionCache.nbtWrite = ReflectionCache.nbtIoClass.getMethod("a", ReflectionCache.nbtCompoundClass, java.nio.file.Path.class);
                } catch (NoSuchMethodException e3) {
                    ReflectionCache.nbtWrite = ReflectionCache.nbtIoClass.getMethod("a", ReflectionCache.nbtCompoundClass, DataOutputStream.class);
                }
            }
        }
    }
    private static Object getUnlimitedAccounter() throws Exception {
        try {
            Field unlimitedHeap = ReflectionCache.nbtAccounterClass.getField("unlimitedHeap");
            return unlimitedHeap.get(null);
        } catch (NoSuchFieldException e1) {
            try {
                Field unlimited = ReflectionCache.nbtAccounterClass.getField("UNLIMITED");
                return unlimited.get(null);
            } catch (NoSuchFieldException e2) {
                try {
                    Field a = ReflectionCache.nbtAccounterClass.getField("a");
                    return a.get(null);
                } catch (NoSuchFieldException e3) {
                    Method create = ReflectionCache.nbtAccounterClass.getMethod("create", long.class);
                    return create.invoke(null, Long.MAX_VALUE);
                }
            }
        }
    }

    private static Class<?> findClass(String... names) {
        for (String name : names) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
    }

    private static Method findMethod(Class<?> clazz, String[] names, Class<?>... paramTypes) {
        for (String name : names) {
            try {
                return clazz.getMethod(name, paramTypes);
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private static String getServerVersionInternal() {
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        return packageName.substring(packageName.lastIndexOf('.') + 1);
    }

    public static ViewInvAdmin.OfflineInventoryData readPlayerInventory(File playerFile) throws Exception {
        ViewInvAdmin.OfflineInventoryData data = new ViewInvAdmin.OfflineInventoryData();
        data.playerFile = playerFile;

        if (DEBUG) logDebug("Reading player file: " + playerFile.getName());

        Object nbtTagCompound = readNBTFromFile(playerFile);
        data.nbtCompound = nbtTagCompound;

        if (DEBUG) logDebug("NBT compound class: " + (nbtTagCompound != null ? nbtTagCompound.getClass().getName() : "null"));

        Object inventoryList = getNBTList(nbtTagCompound, "Inventory");

        if (inventoryList != null) {
            int listSize = getNBTListSize(inventoryList);
            if (DEBUG) logDebug("Inventory list size: " + listSize);

            for (int i = 0; i < listSize; i++) {
                Object itemTag = getNBTListElement(inventoryList, i);
                byte slot = getNBTByte(itemTag, "Slot");

                if (slot >= 0 && slot < 41 || slot == -106) {
                    ItemStack item = itemStackFromNBT(itemTag);

                    if (slot >= 0 && slot <= 35) {
                        data.inventory[slot] = item;
                    } else if (slot >= 100 && slot <= 103) {
                        data.armor[slot - 100] = item;
                    } else if (slot == -106) {
                        data.offhand = item;
                    }
                }
            }
        } else if (DEBUG) {
            logDebug("WARNING: Inventory list is null!");
        }

        Object enderList = getNBTList(nbtTagCompound, "EnderItems");

        if (enderList != null) {
            int enderSize = getNBTListSize(enderList); 
            if (DEBUG) logDebug("EnderItems list size: " + enderSize);

            for (int i = 0; i < enderSize; i++) {
                Object itemTag = getNBTListElement(enderList, i);
                byte slot = getNBTByte(itemTag, "Slot");

                if (slot >= 0 && slot < 27) {
                    data.enderChest[slot] = itemStackFromNBT(itemTag);
                }
            }
        } else if (DEBUG) {
            logDebug("WARNING: EnderItems list is null!");
        }

        return data;
    }

    public static void writePlayerInventory(File playerFile, ViewInvAdmin.OfflineInventoryData data) throws Exception {
        try {
            Object nbtTagCompound = data.nbtCompound;

            if (nbtTagCompound == null) {
                throw new Exception("NBT compound is null - cannot save data!");
            }

            Object inventoryList = createNBTList();

            for (int i = 0; i < 36; i++) {
                if (data.inventory[i] != null) {
                    Object itemTag = itemStackToNBT(data.inventory[i]);
                    setNBTByte(itemTag, "Slot", (byte) i);
                    addToNBTList(inventoryList, itemTag);
                }
            }

            for (int i = 0; i < 4; i++) {
                if (data.armor[i] != null) {
                    Object itemTag = itemStackToNBT(data.armor[i]);
                    setNBTByte(itemTag, "Slot", (byte) (100 + i));
                    addToNBTList(inventoryList, itemTag);
                }
            }

            if (data.offhand != null) {
                Object itemTag = itemStackToNBT(data.offhand);
                setNBTByte(itemTag, "Slot", (byte) -106);
                addToNBTList(inventoryList, itemTag);
            }

            setNBTList(nbtTagCompound, "Inventory", inventoryList);

            Object enderList = createNBTList();
            for (int i = 0; i < 27; i++) {
                if (data.enderChest[i] != null) {
                    Object itemTag = itemStackToNBT(data.enderChest[i]);
                    setNBTByte(itemTag, "Slot", (byte) i);
                    addToNBTList(enderList, itemTag);
                }
            }

            setNBTList(nbtTagCompound, "EnderItems", enderList);
            writeNBTToFile(playerFile, nbtTagCompound);

            if (DEBUG) logDebug("Successfully wrote player data to file");

        } catch (Exception e) {
            if (DEBUG) {
                logDebug("ERROR during write: " + e.getMessage());
                e.printStackTrace();
            }
            throw new Exception("Failed to write player data: " + e.getMessage(), e);
        }
    }

    private static Object readNBTFromFile(File file) throws Exception {
        try {
            if (ReflectionCache.nbtRead.getParameterTypes()[0] == java.nio.file.Path.class) {
                return ReflectionCache.nbtRead.invoke(null, file.toPath(), ReflectionCache.unlimitedAccounter);
            } else if (ReflectionCache.nbtRead.getParameterTypes()[0] == InputStream.class) {
                try (InputStream is = new FileInputStream(file)) {
                    return ReflectionCache.nbtRead.invoke(null, is, ReflectionCache.unlimitedAccounter);
                }
            } else {
                try (DataInputStream dis = new DataInputStream(new GZIPInputStream(new FileInputStream(file)))) {
                    return ReflectionCache.nbtRead.invoke(null, dis);
                }
            }
        } catch (Exception e) {
            throw new Exception("Failed to read NBT from file: " + e.getMessage(), e);
        }
    }

    private static void writeNBTToFile(File file, Object nbtTag) throws Exception {
        try {
            Class<?>[] paramTypes = ReflectionCache.nbtWrite.getParameterTypes();
            if (paramTypes[1] == java.nio.file.Path.class) {
                ReflectionCache.nbtWrite.invoke(null, nbtTag, file.toPath());
            } else if (paramTypes[1] == OutputStream.class) {
                try (OutputStream os = new FileOutputStream(file)) {
                    ReflectionCache.nbtWrite.invoke(null, nbtTag, os);
                }
            } else {
                try (DataOutputStream dos = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(file)))) {
                    ReflectionCache.nbtWrite.invoke(null, nbtTag, dos);
                }
            }
        } catch (Exception e) {
            throw new Exception("Failed to write NBT to file: " + e.getMessage(), e);
        }
    }

    private static ItemStack itemStackFromNBT(Object nbtTag) throws Exception {
        try {
            String id = getNBTString(nbtTag, "id");
            int count = 1;

            try {
                Object result = ReflectionCache.compoundGetInt.invoke(nbtTag, "count");
                count = unwrapOptional(result, 1);
            } catch (Exception e) {
                try {
                    byte countByte = getNBTByte(nbtTag, "Count");
                    count = countByte > 0 ? countByte : 1;
                } catch (Exception e2) {
                    count = 1;
                }
            }

            if (count <= 0) count = 1;

            short damage = getNBTShort(nbtTag, "Damage");

            org.bukkit.Material material = getMaterialFromId(id);
            if (material == null || material == org.bukkit.Material.AIR) {
                return null;
            }

            ItemStack item = new ItemStack(material, count);

            if (damage > 0 && item.getType().getMaxDurability() > 0) {
                item.setDurability(damage);
            }

            Object tag = getNBTCompound(nbtTag, "tag");
            if (tag != null) {
                applyNBTToItem(item, tag);
            }

            return item;

        } catch (Exception e) {
            if (DEBUG) {
                logDebug("Error creating ItemStack: " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        }
    }
    
    private static Object itemStackToNBT(ItemStack item) throws Exception {
        Object nbtTag = createNBTCompound();

        String materialId = getMaterialId(item.getType());
        setNBTString(nbtTag, "id", materialId);

        int amount = item.getAmount();

        boolean modernFormatSet = false;
        if (ReflectionCache.compoundPutInt != null) {
            try {
                ReflectionCache.compoundPutInt.invoke(nbtTag, "count", amount);
                modernFormatSet = true;
            } catch (Exception ignored) {
            }
        }

        try {
            setNBTByte(nbtTag, "Count", (byte) amount);
        } catch (Exception e) {
            if (!modernFormatSet) {
                throw new Exception("Failed to save item count in any format!");
            }
        }

        if (item.getDurability() > 0) {
            setNBTShort(nbtTag, "Damage", item.getDurability());
        }

        if (item.hasItemMeta()) {
            Object tagCompound = createNBTCompound();
            applyItemMetaToNBT(item, tagCompound);
            setNBTCompound(nbtTag, "tag", tagCompound);
        }

        return nbtTag;
    }

    private static void applyNBTToItem(ItemStack item, Object nbtTag) throws Exception {
        Object display = getNBTCompound(nbtTag, "display");
        if (display != null) {
            String name = getNBTString(display, "Name");
            if (name != null && !name.isEmpty()) {
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(name);
                    item.setItemMeta(meta);
                }
            }

            Object loreList = getNBTList(display, "Lore");
            if (loreList != null) {
                java.util.List<String> lore = new java.util.ArrayList<>();
                int size = getNBTListSize(loreList);
                for (int i = 0; i < size; i++) {
                    Object loreTag = getNBTListElement(loreList, i);
                    String loreLine = getNBTString(loreTag, "");
                    if (loreLine != null) {
                        lore.add(loreLine);
                    }
                }
                if (!lore.isEmpty()) {
                    org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                    }
                }
            }
        }
    }

    private static void applyItemMetaToNBT(ItemStack item, Object nbtTag) throws Exception {
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        Object display = createNBTCompound();

        if (meta.hasDisplayName()) {
            setNBTString(display, "Name", meta.getDisplayName());
        }

        if (meta.hasLore()) {
            Object loreList = createNBTList();
            for (String line : meta.getLore()) {
                Object loreTag = createNBTString(line);
                addToNBTList(loreList, loreTag);
            }
            setNBTList(display, "Lore", loreList);
        }

        setNBTCompound(nbtTag, "display", display);
    }

    private static <T> T unwrapOptional(Object result, T defaultValue) {
        if (result == null) return defaultValue;

        if (ReflectionCache.usesOptional && result.getClass().getName().contains("Optional")) {
            try {
                Method isPresent = result.getClass().getMethod("isPresent");
                if ((Boolean) isPresent.invoke(result)) {
                    Method get = result.getClass().getMethod("get");
                    return (T) get.invoke(result);
                }
            } catch (Exception ignored) {
            }
            return defaultValue;
        }

        return (T) result;
    }

    private static org.bukkit.Material getMaterialFromId(String id) {
        if (id == null) return null;
        id = id.replace("minecraft:", "").toUpperCase();
        try {
            return org.bukkit.Material.valueOf(id);
        } catch (IllegalArgumentException e) {
            return getLegacyMaterial(id);
        }
    }

    private static org.bukkit.Material getLegacyMaterial(String id) {
        switch (id.toUpperCase()) {
            case "GRASS":
                try {
                    return org.bukkit.Material.valueOf("GRASS_BLOCK");
                } catch (IllegalArgumentException e) {
                    return org.bukkit.Material.STONE;
                }
            case "WOOD":
                try {
                    return org.bukkit.Material.valueOf("OAK_WOOD");
                } catch (IllegalArgumentException e) {
                    return org.bukkit.Material.STONE;
                }
            case "LOG":
                try {
                    return org.bukkit.Material.valueOf("OAK_LOG");
                } catch (IllegalArgumentException e) {
                    return org.bukkit.Material.STONE;
                }
            case "STONE":
                return org.bukkit.Material.STONE;
            default:
                try {
                    org.bukkit.Material material = org.bukkit.Material.matchMaterial(id);
                    return material != null ? material : org.bukkit.Material.STONE;
                } catch (Exception e) {
                    return org.bukkit.Material.STONE;
                }
        }
    }

    private static String getMaterialId(org.bukkit.Material material) {
        return "minecraft:" + material.name().toLowerCase();
    }

    private static Object createNBTCompound() throws Exception {
        return ReflectionCache.nbtCompoundClass.getDeclaredConstructor().newInstance();
    }

    private static Object createNBTList() throws Exception {
        return ReflectionCache.nbtListClass.getDeclaredConstructor().newInstance();
    }

    private static Object createNBTString(String value) throws Exception {
        if (ReflectionCache.stringCreate != null) {
            return ReflectionCache.stringCreate.invoke(null, value);
        }
        return ReflectionCache.nbtStringClass.getConstructor(String.class).newInstance(value);
    }

    private static Object getNBTList(Object compound, String key) throws Exception {
        if (ReflectionCache.compoundGetList == null) return null;

        Object result;
        if (ReflectionCache.compoundGetList.getParameterCount() == 2) {
            result = ReflectionCache.compoundGetList.invoke(compound, key, 10);
        } else {
            result = ReflectionCache.compoundGetList.invoke(compound, key);
        }

        return unwrapOptional(result, null);
    }

    private static void setNBTList(Object compound, String key, Object list) throws Exception {
        ReflectionCache.compoundPut.invoke(compound, key, list);
    }

    private static int getNBTListSize(Object list) throws Exception {
        if (list == null) return 0;

        Object result = ReflectionCache.listSize.invoke(list);
        return ((Number) result).intValue();
    }

    private static Object getNBTListElement(Object list, int index) throws Exception {
        if (list == null) return null;
        return ReflectionCache.listGet.invoke(list, index);
    }

    private static void addToNBTList(Object list, Object element) throws Exception {
        if (list == null || element == null) return;

        if (ReflectionCache.listAdd.getParameterCount() == 1) {
            ReflectionCache.listAdd.invoke(list, element);
        } else {
            int size = getNBTListSize(list);
            ReflectionCache.listAdd.invoke(list, size, element);
        }
    }

    private static String getNBTString(Object compound, String key) throws Exception {
        try {
            Object result = ReflectionCache.compoundGetString.invoke(compound, key);
            return unwrapOptional(result, "");
        } catch (Exception e) {
            return "";
        }
    }

    private static void setNBTString(Object compound, String key, String value) throws Exception {
        if (ReflectionCache.compoundPutString != null) {
            ReflectionCache.compoundPutString.invoke(compound, key, value);
        } else {
            Object nbtString = createNBTString(value);
            ReflectionCache.compoundPut.invoke(compound, key, nbtString);
        }
    }

    private static byte getNBTByte(Object compound, String key) throws Exception {
        try {
            Object result = ReflectionCache.compoundGetByte.invoke(compound, key);
            Number num = unwrapOptional(result, (byte) 0);
            return num.byteValue();
        } catch (Exception e) {
            return 0;
        }
    }

    private static void setNBTByte(Object compound, String key, byte value) throws Exception {
        ReflectionCache.compoundPutByte.invoke(compound, key, value);
    }

    private static short getNBTShort(Object compound, String key) throws Exception {
        try {
            Object result = ReflectionCache.compoundGetShort.invoke(compound, key);
            Number num = unwrapOptional(result, (short) 0);
            return num.shortValue();
        } catch (Exception e) {
            return 0;
        }
    }

    private static Class<?> getNBTBaseClassObject() throws ClassNotFoundException {
        try {
            return Class.forName("net.minecraft.nbt.Tag");
        } catch (ClassNotFoundException e) {
            try {
                return Class.forName("net.minecraft.nbt.NBTBase");
            } catch (ClassNotFoundException e2) {
                String version = getServerVersion();
                return Class.forName("net.minecraft.server." + version + ".NBTBase");
            }
        }
    }

    private static void setNBTShort(Object compound, String key, short value) throws Exception {
        ReflectionCache.compoundPutShort.invoke(compound, key, value);
    }

    private static Object getNBTCompound(Object compound, String key) throws Exception {
        try {
            return ReflectionCache.compoundGetCompound.invoke(compound, key);
        } catch (Exception e) {
            return null;
        }
    }

    private static void setNBTCompound(Object compound, String key, Object value) throws Exception {
        ReflectionCache.compoundPut.invoke(compound, key, value);
    }
    private static String getServerVersion() {
        return ReflectionCache.serverVersion;
    }

    private static void logDebug(String message) {
        if (DEBUG) {
            ItemDisplay.Log("[NBT DEBUG] " + message, "print");
        }
    }

}
