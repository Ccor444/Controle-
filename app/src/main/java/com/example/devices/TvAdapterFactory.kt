package com.example.devices

import com.example.model.TvBrand

object TvAdapterFactory {
    fun createAdapter(brand: TvBrand): TvAdapter {
        return when (brand) {
            TvBrand.ROKU -> RokuAdapter()
            TvBrand.SAMSUNG -> SamsungAdapter()
            TvBrand.LG -> LgAdapter()
            TvBrand.ANDROID_TV -> AndroidTvAdapter()
            TvBrand.GENERIC -> RokuAdapter() // Default to ECP REST protocol fallback
        }
    }
}
