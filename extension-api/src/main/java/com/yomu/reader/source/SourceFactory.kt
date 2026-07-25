package com.yomu.reader.source

/**
 * Entry point an extension APK exposes to produce one or more [Source]s.
 *
 * The extension's manifest declares the factory (or a [Source]) class via
 *   <meta-data android:name="com.yomu.reader.extension.class" android:value=".MyFactory"/>
 * and the host instantiates it with a no-arg constructor, then calls [createSources].
 */
interface SourceFactory {
    fun createSources(): List<Source>
}
