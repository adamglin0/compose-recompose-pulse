package com.adamglin.recompose.pulse.gradle

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

abstract class PulseGradleExtension @Inject constructor(objects: ObjectFactory) {
    val enabled: Property<Boolean> = objects.property(Boolean::class.javaObjectType).convention(true)
    val debugOnly: Property<Boolean> = objects.property(Boolean::class.javaObjectType).convention(true)
    val includePackages: ListProperty<String> = objects.listProperty(String::class.java).convention(emptyList())
    val excludePackages: ListProperty<String> = objects.listProperty(String::class.java).convention(emptyList())
}
