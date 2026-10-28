package com.example.iiserautologin

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.core.content.ContextCompat

class IISERQuickTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        if (!CredentialStore.hasCredentials(this)) {
            Toast.makeText(
                this,
                "Configure your IISER account in Settings first.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (IISERAuthService.isRunning) {
            stopService(Intent(this, IISERAuthService::class.java))
        } else {
            ContextCompat.startForegroundService(
                this,
                Intent(this, IISERAuthService::class.java)
            )
        }

        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return

        tile.icon = Icon.createWithResource(
            this,
            R.drawable.ic_iiser_tile
        )
        tile.label = "IISER Login"

        if (IISERAuthService.isRunning) {
            tile.state = Tile.STATE_ACTIVE
            tile.contentDescription = "IISER Auto Login is active"
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.contentDescription = "IISER Auto Login is inactive"
        }

        tile.updateTile()
    }

    companion object {
        fun refreshTile(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                TileService.requestListeningState(
                    context,
                    ComponentName(context, IISERQuickTileService::class.java)
                )
            }
        }
    }
}
