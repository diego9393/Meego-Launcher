package com.solarimaginglab.meego.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import org.xmlpull.v1.XmlPullParser

class IconPackManager(private val context: Context) {
    fun getIconFromPack(packageName: String, iconPackPackage: String): Drawable? {
        val pm = context.packageManager
        try {
            val res = pm.getResourcesForApplication(iconPackPackage)
            val id = res.getIdentifier("appfilter", "xml", iconPackPackage)
            if (id > 0) {
                val xpp = res.getXml(id)
                while (xpp.eventType != XmlPullParser.END_DOCUMENT) {
                    if (xpp.eventType == XmlPullParser.START_TAG && xpp.name == "item") {
                        val component = xpp.getAttributeValue(null, "component")
                        val drawableName = xpp.getAttributeValue(null, "drawable")
                        if (component.contains(packageName)) {
                            val drawableId = res.getIdentifier(drawableName, "drawable", iconPackPackage)
                            if (drawableId > 0) return res.getDrawable(drawableId, null)
                        }
                    }
                    xpp.next()
                }
            }
        } catch (e: Exception) { }
        return null
    }

    fun getInstalledIconPacks(): List<String> {
        val pm = context.packageManager
        val intent = Intent("com.novalauncher.THEME")
        return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL).map { it.activityInfo.packageName }
    }
}