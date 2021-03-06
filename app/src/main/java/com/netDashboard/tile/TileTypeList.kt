package com.netDashboard.tile

import com.netDashboard.tile.types.button.ButtonTile
import com.netDashboard.tile.types.slider.SliderTile

class TileTypeList {
    companion object {
        fun get(): List<Tile> {
            return listOf(
                ButtonTile(),
                SliderTile()
            )
        }

        fun getTileById(id: Int): Tile {
            return get()[id]
        }

        fun String.toTileType(): Class<*>? {
            return when (this) {
                ButtonTile().type -> ButtonTile::class.java
                SliderTile().type -> SliderTile::class.java
                else -> null
            }
        }
    }
}