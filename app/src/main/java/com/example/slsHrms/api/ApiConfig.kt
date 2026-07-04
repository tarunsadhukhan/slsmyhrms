package com.example.slsHrms.api

import android.content.Context
import android.content.SharedPreferences

object ApiConfig {

    private const val PREF_NAME = "api_config"
    private const val KEY_BASE_URL = "base_url"
    // The user configures one URL. The database (tenant) name can be given as a
    // subdomain or as the last path segment; both of these mean database "sls":
    //     http://sls.192.168.0.133:8000      (subdomain on an IP)
    //     http://sls.vowerp.co.in:8000       (subdomain on a domain)
    //     http://192.168.0.133:8000/sls      (path segment)
    // The database name is sent to the backend in the X-Tenant header. For the
    // IP+subdomain case the "sls." label is stripped from the host we actually
    // connect to, because "sls.<ip>" is not DNS-resolvable; for a real domain
    // the host is kept as-is (wildcard DNS resolves it).
    private const val DEFAULT_URL = "http://sls.192.168.0.133:8000"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /** The full configured URL as typed — used for display/editing in Settings. */
    fun getConfiguredUrl(context: Context): String {
        return getPrefs(context).getString(KEY_BASE_URL, DEFAULT_URL) ?: DEFAULT_URL
    }

    /** Host root the app actually connects to (Retrofit base URL, no tenant). */
    fun getBaseUrl(context: Context): String = parse(getConfiguredUrl(context)).baseUrl

    /** Database/tenant name, sent to the backend via the X-Tenant header. */
    fun getTenant(context: Context): String = parse(getConfiguredUrl(context)).tenant

    fun saveBaseUrl(context: Context, url: String) {
        var formattedUrl = url.trim()
        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
            formattedUrl = "http://$formattedUrl"
        }
        if (!formattedUrl.endsWith("/")) {
            formattedUrl = "$formattedUrl/"
        }
        getPrefs(context).edit().putString(KEY_BASE_URL, formattedUrl).apply()
    }

    // ── URL parsing ──────────────────────────────────────────────────────────

    private data class Parsed(val baseUrl: String, val tenant: String)

    private fun isIpv4(host: String): Boolean {
        val parts = host.split(".")
        if (parts.size != 4) return false
        return parts.all { p -> p.toIntOrNull()?.let { it in 0..255 } == true }
    }

    private fun parse(configured: String): Parsed {
        var raw = configured.trim()
        if (!raw.startsWith("http://") && !raw.startsWith("https://")) {
            raw = "http://$raw"
        }
        raw = raw.trimEnd('/')

        val schemeEnd = raw.indexOf("://") + 3
        val scheme = raw.substring(0, schemeEnd)            // "http://"
        val afterScheme = raw.substring(schemeEnd)          // "sls.192.168.0.133:8000/path"

        val slash = afterScheme.indexOf('/')
        val authority = if (slash >= 0) afterScheme.substring(0, slash) else afterScheme
        val path = if (slash >= 0) afterScheme.substring(slash + 1).trim('/') else ""

        val colon = authority.indexOf(':')
        val host = if (colon >= 0) authority.substring(0, colon) else authority
        val port = if (colon >= 0) authority.substring(colon) else ""   // ":8000"

        var tenant = ""
        var reachableHost = host

        when {
            // 1) Tenant given as the last path segment: ".../sls"
            path.isNotEmpty() -> {
                tenant = path.substringAfterLast('/')
            }
            // 2) Pure IP, no tenant in the URL.
            isIpv4(host) -> {
                // leave tenant empty
            }
            // 3) "<label>.<ipv4>"  ->  tenant = label, connect to the bare IP.
            host.contains('.') && isIpv4(host.substringAfter('.')) -> {
                tenant = host.substringBefore('.')
                reachableHost = host.substringAfter('.')
            }
            // 4) "<sub>.<domain...>"  ->  tenant = leading label, keep host.
            host.contains('.') -> {
                tenant = host.substringBefore('.')
                reachableHost = host
            }
        }

        return Parsed("$scheme$reachableHost$port/", tenant)
    }
}
