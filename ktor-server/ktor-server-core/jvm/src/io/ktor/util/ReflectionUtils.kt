/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.util

import java.lang.reflect.*
import kotlin.reflect.*
import kotlin.reflect.jvm.*

@PublishedApi
internal fun KType.toJavaType(): Type {
    val classifier = classifier

    return when {
        arguments.isNotEmpty() -> JavaTypeAdapter(this)
        classifier is KClass<*> -> classifier.javaObjectType
        classifier is KTypeParameter -> {
            error("KType parameter classifier is not supported")
        }
        else -> error("Unsupported type $this")
    }
}

private class JavaTypeAdapter(val type: KType) : ParameterizedType {
    override fun getRawType(): Type {
        return type.jvmErasure.javaObjectType
    }

    override fun getOwnerType(): Type? = null

    override fun getActualTypeArguments(): Array<Type> {
        return type.arguments.map {
            when (it.variance) {
                null, KVariance.IN, KVariance.OUT -> BoundTypeAdapter(it)
                else -> it.type!!.toJavaType()
            }
        }.toTypedArray()
    }

    override fun hashCode(): Int = actualTypeArguments.contentHashCode() xor
        0 xor
        rawType.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other === this) return true

        return other is ParameterizedType &&
            other.ownerType == null &&
            other.rawType == rawType
            && actualTypeArguments.contentEquals(other.actualTypeArguments)
    }

    override fun toString(): String {
        return type.toString()
    }
}

private class BoundTypeAdapter(val type: KTypeProjection) : WildcardType {
    override fun getLowerBounds(): Array<Type> {
        return when (type.variance) {
            null, KVariance.OUT -> arrayOf(Any::class.java)
            else -> arrayOf(type.type!!.toJavaType())
        }
    }

    override fun getUpperBounds(): Array<Type> {
        return when (type.variance) {
            null, KVariance.IN -> arrayOf(Any::class.java)
            else -> arrayOf(type.type!!.toJavaType())
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is WildcardType &&
            lowerBounds.contentEquals(other.lowerBounds) &&
            upperBounds.contentEquals(other.upperBounds)
    }

    override fun hashCode(): Int {
        return lowerBounds.contentHashCode() xor upperBounds.contentHashCode()
    }

    override fun toString(): String {
        return type.toString()
    }
}
