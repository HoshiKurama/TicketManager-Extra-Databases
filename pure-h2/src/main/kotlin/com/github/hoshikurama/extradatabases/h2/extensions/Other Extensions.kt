package com.github.hoshikurama.extradatabases.h2.extensions

import java.util.concurrent.CompletableFuture

fun <T> List<CompletableFuture<T>>.flatten(): CompletableFuture<List<T>> {
    return CompletableFuture.allOf(*this.toTypedArray())
        .thenApplyAsync { this.map { it.join() } }
}