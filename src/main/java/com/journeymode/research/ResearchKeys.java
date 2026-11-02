package com.journeymode.research;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.HolderLookup;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.OminousBottleAmplifier;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * What a piece of research is saved under. Most items are researched as just the item
 * ({@code minecraft:oak_log}). Some items come in kinds that are different things to research, so
 * the kind is added after a {@code #}:
 * <ul>
 *   <li>potions, splash and lingering potions and tipped arrows, by potion
 *   ({@code minecraft:potion#minecraft:strong_healing});</li>
 *   <li>enchanted books, by their enchantments and levels
 *   ({@code minecraft:enchanted_book#minecraft:sharpness@5});</li>
 *   <li>ominous bottles, by strength ({@code minecraft:ominous_bottle#2}).</li>
 *   <li>spell scrolls and books from Spell Engine (and mods built on it, like Wizards): the spell lives in the
 *   item's data, so the key holds all of it ({@code spell_engine:spell_scroll#@{...}}) and the item given out
 *   is rebuilt from it.</li>
 * </ul>
 * Those items are never researched as just the item, since that would be a potion with no potion in it.
 */
public final class ResearchKeys {
	private static final char KIND = '#';
	/** Starts the kind of an item that is researched by its whole component data, rather than one thing in it. */
	private static final String DATA = "@";
	/** Starts the kind of a spell scroll: the id of the spell on it. Only the spell counts, not how the scroll was made. */
	private static final String SPELL = "$";
	private static final String SPELL_CONTAINER = "spell_engine:spell_container";
	/** Items whose spell (or whatever else makes each one different) is kept in their data components. */
	private static final Set<String> BY_DATA = Set.of("spell_engine:spell_scroll", "spell_engine:spell_book");

	private ResearchKeys() {
	}

	/** The key the stack is researched under. */
	public static String of(ItemStack stack) {
		String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		String kind = kind(stack);
		return kind == null ? id : id + KIND + kind;
	}

	/** The item a key is about, or null if it is not an item that exists now (a removed mod's, say). */
	public static Item itemOf(String key) {
		int at = key.indexOf(KIND);
		Identifier id = Identifier.tryParse(at < 0 ? key : key.substring(0, at));
		Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
		return item == null || item == Items.AIR ? null : item;
	}

	/** Whether the item is one of those that are researched by kind and never as just the item. */
	private static boolean byKind(Item item) {
		return byData(item) || item == Items.ENCHANTED_BOOK || item == Items.OMINOUS_BOTTLE || item == Items.POTION
				|| item == Items.SPLASH_POTION || item == Items.LINGERING_POTION || item == Items.TIPPED_ARROW;
	}

	private static boolean byData(Item item) {
		return BY_DATA.contains(BuiltInRegistries.ITEM.getKey(item).toString());
	}

	/** The id of the one spell in the item data (given as a component patch), or null if there is not exactly one. */
	private static String spellOn(CompoundTag patch) {
		if (patch.get(SPELL_CONTAINER) instanceof CompoundTag container && container.get("spell_ids") instanceof ListTag ids
				&& ids.size() == 1) {
			return ids.get(0).asString().orElse(null);
		}
		return null;
	}

	/**
	 * Makes a spell scroll for a spell by asking Spell Engine to, so the scroll gets the same rarity, model and name
	 * as one the game makes. Returns an empty stack if the spell does not exist or Spell Engine's code is not as expected.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private static ItemStack scrollOf(Item item, String spell, HolderLookup.Provider registries) {
		try {
			Identifier id = Identifier.tryParse(spell);
			if (id == null || !(registries instanceof RegistryAccess access)) {
				return ItemStack.EMPTY;
			}
			Class<?> scrollItem = Class.forName("net.spell_engine.item.ScrollItem");
			Class<?> spellRegistry = Class.forName("net.spell_engine.api.spell.registry.SpellRegistry");
			ResourceKey<Registry<Object>> registryKey = (ResourceKey<Registry<Object>>) spellRegistry.getField("KEY").get(null);
			Optional<Registry<Object>> registry = access.lookup(registryKey);
			if (registry.isEmpty()) {
				return ItemStack.EMPTY;
			}
			Optional<Holder.Reference<Object>> holder = registry.get().get(ResourceKey.create(registryKey, id));
			if (holder.isEmpty()) {
				return ItemStack.EMPTY;
			}
			Object pool = scrollItem.getMethod("resolveSpellPool", Registry.class, Holder.class).invoke(null, registry.get(), holder.get());
			ItemStack stack = new ItemStack(item);
			scrollItem.getMethod("applySpell", ItemStack.class, Holder.class, TagKey.class).invoke(null, stack, holder.get(), pool);
			return stack;
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			return ItemStack.EMPTY;
		}
	}

	/** Which kind of its item the stack is, or null if it has none. */
	private static String kind(ItemStack stack) {
		Item item = stack.getItem();
		if (byData(item)) {
			DataResult<Tag> encoded = DataComponentPatch.CODEC.encodeStart(NbtOps.INSTANCE, stack.getComponentsPatch());
			Tag tag = encoded.result().orElse(null);
			String spell = tag instanceof CompoundTag patch ? spellOn(patch) : null;
			if (spell != null && BuiltInRegistries.ITEM.getKey(item).getPath().equals("spell_scroll")) {
				return SPELL + spell;
			}
			return tag instanceof CompoundTag compound && !compound.isEmpty() ? DATA + compound : null;
		}
		if (item == Items.ENCHANTED_BOOK) {
			ItemEnchantments enchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
			if (enchantments == null || enchantments.isEmpty()) {
				return null;
			}
			List<String> parts = new ArrayList<>();
			for (var entry : enchantments.entrySet()) {
				parts.add(entry.getKey().getRegisteredName() + "@" + entry.getIntValue());
			}
			parts.sort(Comparator.naturalOrder());
			return String.join(",", parts);
		}
		if (item == Items.OMINOUS_BOTTLE) {
			OminousBottleAmplifier amplifier = stack.get(DataComponents.OMINOUS_BOTTLE_AMPLIFIER);
			return amplifier == null ? null : String.valueOf(amplifier.value());
		}
		if (byKind(item)) {
			PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
			return contents == null ? null : contents.potion().map(Holder::getRegisteredName).orElse(null);
		}
		return null;
	}

	/**
	 * A single item of the kind a key describes, or an empty stack if the key is not something that can be
	 * made now (the item or the enchantment or potion no longer exists).
	 */
	public static ItemStack stackOf(String key, HolderLookup.Provider registries) {
		Item item = itemOf(key);
		if (item == null) {
			return ItemStack.EMPTY;
		}
		int at = key.indexOf(KIND);
		if (at < 0) {
			return byKind(item) ? ItemStack.EMPTY : new ItemStack(item);
		}
		String kind = key.substring(at + 1);
		if (kind.startsWith(SPELL)) {
			return scrollOf(item, kind.substring(SPELL.length()), registries);
		}
		ItemStack stack = new ItemStack(item);
		try {
			if (byData(item)) {
				CompoundTag tag = TagParser.parseCompoundFully(kind.substring(DATA.length()));
				DataComponentPatch patch = DataComponentPatch.CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(null);
				if (patch == null) {
					return ItemStack.EMPTY;
				}
				stack.applyComponents(patch);
			} else if (item == Items.ENCHANTED_BOOK) {
				ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
				for (String part : kind.split(",")) {
					int split = part.lastIndexOf('@');
					Identifier id = Identifier.tryParse(part.substring(0, split));
					Optional<Holder.Reference<Enchantment>> enchantment = id == null ? Optional.empty()
							: registries.lookupOrThrow(Registries.ENCHANTMENT).get(ResourceKey.create(Registries.ENCHANTMENT, id));
					if (enchantment.isEmpty()) {
						return ItemStack.EMPTY;
					}
					enchantments.set(enchantment.get(), Integer.parseInt(part.substring(split + 1)));
				}
				stack.set(DataComponents.STORED_ENCHANTMENTS, enchantments.toImmutable());
			} else if (item == Items.OMINOUS_BOTTLE) {
				stack.set(DataComponents.OMINOUS_BOTTLE_AMPLIFIER, new OminousBottleAmplifier(Integer.parseInt(kind)));
			} else {
				Identifier id = Identifier.tryParse(kind);
				Optional<Holder.Reference<Potion>> potion = id == null ? Optional.empty() : BuiltInRegistries.POTION.get(id);
				if (potion.isEmpty()) {
					return ItemStack.EMPTY;
				}
				stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion.get()));
			}
		} catch (RuntimeException | CommandSyntaxException e) {
			return ItemStack.EMPTY;
		}
		return stack;
	}
}
