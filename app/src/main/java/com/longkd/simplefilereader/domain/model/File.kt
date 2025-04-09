package com.longkd.simplefilereader.domain.model

data class File(
    val name: String,
    val dateModifier: String,
    val size: String,
    val path: String,
    val type: FileType
)