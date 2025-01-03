package foundation.esoteric.fireworkwarslobby.util

import dev.triumphteam.gui.builder.item.ItemBuilder
import foundation.esoteric.fireworkwarscore.language.LanguageManager
import foundation.esoteric.fireworkwarscore.language.Message

fun ItemBuilder.name(message: Message, vararg args: Any?): ItemBuilder {
    return this.name(
        LanguageManager.globalInstance.getDefaultMessage(message, *args))
}

fun ItemBuilder.lore(message: Message, vararg args: Any?): ItemBuilder {
    return this.lore(
        LanguageManager.globalInstance.getDefaultMessage(message, *args))
}