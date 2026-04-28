package com.example.applicationmobilesupervisiondeslivraisons

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object SupabaseClientProvider {

    val client = createSupabaseClient(
        supabaseUrl = "https://ahbramrtgcvvmbwkgwyk.supabase.co",  // ← replace
        supabaseKey = "sb_publishable_mk2_gF-1rAqQnVm1fhCFaw_eiuQyLIM"                  // ← replace
    ) {
        install(Auth)
        install(Postgrest)
        install(Realtime)
    }
}