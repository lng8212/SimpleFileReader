package com.longkd.simplefilereader.presentation.listfile.model

import com.longkd.simplefilereader.domain.model.FileDTO

object FileMapper {
    fun FileDTO.toFile(): File {
        return File(
            name = name.substringBeforeLast("."),
            desc = "$dateModifier, $size",
            fileType = type,
            contentUri = uri
        )
    }
}