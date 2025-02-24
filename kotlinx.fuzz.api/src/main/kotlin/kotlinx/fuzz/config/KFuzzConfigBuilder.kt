package kotlinx.fuzz.config

import kotlin.reflect.KProperty

class KFuzzConfigBuilder(
    private val propertiesMap: Map<String, String>,
) {
    private var isBuilt = false

    // FQN --> KFProp
    private val delegatesMap = mutableMapOf<String, KFuzzProperty<*>>()
    private val overrideSteps = mutableListOf<KFuzzConfigImpl.() -> Unit>()
    private val fallbackSteps = mutableListOf<KFuzzConfigImpl.() -> Unit>()
    private val configImpl = KFuzzConfigImpl()

    fun editOverride(editor: KFuzzConfigImpl.() -> Unit): KFuzzConfigBuilder = this.also {
        overrideSteps.add(editor)
    }

    fun editFallback(editor: KFuzzConfigImpl.() -> Unit): KFuzzConfigBuilder = this.also {
        fallbackSteps.add(editor)
    }

    fun build(): KFuzzConfig {
        check(!isBuilt) { "config is already built!" }
        try {
            /*
              To guarantee the priority in the following order...
              1) editOverride
              2) property map
              3) editFallback
              4) default
              ...we can set values in backwards order.
             */
            val delegates = delegatesMap.values
            delegates.forEach { it.setFromDefault() }
            fallbackSteps.forEach { it.invoke(configImpl) }
            delegates.forEach { it.setFromPropertiesMap() }
            overrideSteps.forEach { it.invoke(configImpl) }

            delegates.forEach { it.validate() }
            isBuilt = true
            return configImpl
        } catch (e: Throwable) {
            throw ConfigurationException(e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getPropertyDelegate(propertySelector: KFuzzConfigImpl.() -> KProperty<T>): KFuzzProperty<T> {
        val property = propertySelector(configImpl)

        val kfuzzProperty = delegatesMap[property.toString()] ?: error("no KFProp found for property '${property.name}'")
        return kfuzzProperty as KFuzzProperty<T>
    }

    /**
     * A fuzzing config property. The value is looked up with the following priority:
     *
     * 1) editOverride
     * 2) property map
     * 3) editFallback
     * 4) default
     *
     * @param default throws
     */
    inner class KFuzzProperty<T : Any>(
        val name: String,
        private val fromString: (String) -> T,
        private val intoString: (T) -> String,
        private val validate: (T) -> Unit,
        private val default: T?,
    ) {
        private var value: T? = null
        private val isBuilt get() = this@KFuzzConfigBuilder.isBuilt

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            check(value != null) { "cannot get value, config is not built yet!" }
            return value!!
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            check(!isBuilt) { "cannot set value, config is already built!" }
            this.value = value
        }

        fun setFromPropertiesMap() {
            propertiesMap[name]?.let {
                value = fromString(it)
            }
        }

        fun setFromDefault() {
            default?.let {
                value = it
            }
        }

        fun getStringValue(): String {
            check(isBuilt) { "cannot get string value, config is not built yet!" }
            return intoString(value!!)
        }

        fun validate() {
            check(value != null) { "property '$name' was not set!" }
            validate(value!!)
        }
    }

    inner class KFuzzConfigImpl internal constructor() : KFuzzConfig {
        override val global = GlobalConfigImpl(this@KFuzzConfigBuilder)
        override val target = TargetConfigImpl(this@KFuzzConfigBuilder)
        override val coverage = CoverageConfigImpl(this@KFuzzConfigBuilder)

        // TODO: we have to know which engine to use before building the config...
        // Viable solution for future: build a stub config, check the engine, build a new config.
        // Will need to be careful with instantiating delegates, as they will get validated.
        override val engine = JazzerConfigImpl(this@KFuzzConfigBuilder)

        override fun toPropertiesMap(): Map<String, String> {
            check(isBuilt) { "cannot get properties map, config is not built yet!" }
            return delegatesMap.values.associate { it.name to it.getStringValue() }
        }
    }

    inner class KFuzzPropProvider<T : Any>(
        private val nameSuffix: String,
        private val intoString: (T) -> String,
        private val fromString: (String) -> T,
        private val validate: (T) -> Unit = {},
        private val default: T? = null,
    ) {
        operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): KFuzzProperty<T> {
            val kfuzzProperty = KFuzzProperty(
                name = KFuzzConfig.PROPERTY_NAME_PREFIX + nameSuffix,
                fromString = fromString,
                intoString = intoString,
                validate = validate,
                default = default,
            )
            delegatesMap[property.toString()] = kfuzzProperty
            return kfuzzProperty
        }
    }
}

class ConfigurationException(cause: Throwable?) : IllegalArgumentException("cannot create config: ${cause?.message}", cause)

fun getSystemPropertiesMap(): Map<String, String> = buildMap {
    val properties = System.getProperties()
    val propNames = properties.propertyNames()
    for (name in propNames) {
        val key = name.toString()
        val value = properties.getProperty(key).toString()
        put(key, value)
    }
}
