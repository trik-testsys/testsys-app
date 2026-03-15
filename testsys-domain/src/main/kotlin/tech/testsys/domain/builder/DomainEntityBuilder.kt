package tech.testsys.domain.builder

import java.time.Instant

interface Builder<out T> {
    fun build(): T
}

interface DomainEntityBuilder<out Entity> : Builder<Entity> {

    var id: Long?
    var createdAt: Instant?

    fun createdNow() {
        createdAt = Instant.now()
    }

}

interface DataCapable<Data, DataBuilder : Builder<Data>> {

    var data: Data?

    fun dataBuilder(): DataBuilder

}

inline fun <Data, DataBuilder : Builder<Data>> DataCapable<Data, DataBuilder>.data(builder: DataBuilder.() -> Unit) {
    data = dataBuilder().apply(builder).build()
}

abstract class DomainEntityWithDataBuilder<Entity, Data, DataBuilder : Builder<Data>> :
    DomainEntityBuilder<Entity>, DataCapable<Data, DataBuilder>
{

    override var id: Long? = null
    override var createdAt: Instant? = null
    override var data: Data? = null

}
