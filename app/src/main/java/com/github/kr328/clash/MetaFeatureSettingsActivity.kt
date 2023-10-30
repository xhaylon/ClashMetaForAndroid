package com.github.kr328.clash

import android.R
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.provider.OpenableColumns
import android.widget.Toast
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.design.MetaFeatureSettingsDesign
import com.github.kr328.clash.util.withClash
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume


class MetaFeatureSettingsActivity : BaseActivity<MetaFeatureSettingsDesign>() {
    override suspend fun main() {
        val configuration = withClash { queryOverride(Clash.OverrideSlot.Persist) }

        defer {
            withClash {
                patchOverride(Clash.OverrideSlot.Persist, configuration)
            }
        }

        val design = MetaFeatureSettingsDesign(
            this,
            configuration
        )

        setContentDesign(design)

        while (isActive) {
            select<Unit> {
                events.onReceive {

                }
                design.requests.onReceive {
                    when (it) {
                        MetaFeatureSettingsDesign.Request.ResetOverride -> {
                            if (design.requestResetConfirm()) {
                                defer {
                                    withClash {
                                        clearOverride(Clash.OverrideSlot.Persist)
                                    }
                                }

                                finish()
                            }
                        }
                        MetaFeatureSettingsDesign.Request.ImportGeoIp -> {
                            val intent = Intent(Intent.ACTION_GET_CONTENT)
                            intent.type = "*/*"
                            intent.addCategory(Intent.CATEGORY_OPENABLE)
                            startActivityForResult(
                                intent,
                                MetaFeatureSettingsDesign.Request.ImportGeoIp.ordinal
                            )
                        }
                        MetaFeatureSettingsDesign.Request.ImportGeoSite -> {
                            val intent = Intent(Intent.ACTION_GET_CONTENT)
                            intent.type = "*/*"
                            intent.addCategory(Intent.CATEGORY_OPENABLE)
                            startActivityForResult(
                                intent,
                                MetaFeatureSettingsDesign.Request.ImportGeoSite.ordinal
                            )
                        }
                        MetaFeatureSettingsDesign.Request.ImportCountry -> {
                            val intent = Intent(Intent.ACTION_GET_CONTENT)
                            intent.type = "*/*"
                            intent.addCategory(Intent.CATEGORY_OPENABLE)
                            startActivityForResult(
                                intent,
                                MetaFeatureSettingsDesign.Request.ImportCountry.ordinal
                            )
                        }
                    }
                }
            }
        }
    }

    public val validDatabaseExtensions = listOf(
        ".metadb", ".db", ".dat", ".mmdb"
    )
    @SuppressLint("Range")
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if(resultCode == RESULT_OK) {
            val uri = resultData?.data
            val cursor: Cursor? = uri?.let {
                contentResolver.query(it, null, null, null, null, null)
            }
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayName: String =
                        it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                    val ext = "." + displayName.substringAfterLast(".")
                    if(!validDatabaseExtensions.contains(ext))
                    {
                        MaterialAlertDialogBuilder(this)
                            .setTitle("Unknown Database Format")
                            .setMessage("Only ${validDatabaseExtensions.joinToString("/")} are supported")
                            .setPositiveButton("OK"){ _, _ -> }
                            .show()
                        return
                    }
                    val outputFileName = when (requestCode) {
                        MetaFeatureSettingsDesign.Request.ImportGeoIp.ordinal ->
                            "geoip$ext"
                        MetaFeatureSettingsDesign.Request.ImportGeoSite.ordinal ->
                            "geosite$ext"
                        MetaFeatureSettingsDesign.Request.ImportCountry.ordinal ->
                            "country$ext"
                        else -> ""
                    }
                    if(outputFileName.isEmpty())
                    {
                        Toast.makeText(this, "Bad request", Toast.LENGTH_LONG).show()
                        return
                    }

                    val outputFile = File(File(filesDir, "clash"), outputFileName);
                    contentResolver.openInputStream(uri).use { ins->
                        FileOutputStream(outputFile).use { outs->
                            ins?.copyTo(outs)
                        }
                    }
                    Toast.makeText(this, "$displayName imported", Toast.LENGTH_LONG).show()
                    return
                }
            }
        }
        Toast.makeText(this, "Import failed", Toast.LENGTH_LONG).show()
    }
}