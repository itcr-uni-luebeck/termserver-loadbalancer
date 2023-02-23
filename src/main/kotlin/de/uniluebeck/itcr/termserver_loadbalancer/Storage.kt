package de.uniluebeck.itcr

import de.uniluebeck.itcr.models.Endpoints

class Storage {
    companion object {
        val endpoints by lazy { Endpoints() }
    }
}