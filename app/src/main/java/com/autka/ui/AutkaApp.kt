package com.autka.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.autka.feature.detail.OfferDetailRoute
import com.autka.feature.importcalc.ImportCalculatorRoute
import com.autka.feature.listings.ListingsRoute
import com.autka.feature.map.MapRoute
import com.autka.feature.sourcehealth.SourceHealthRoute

private object Routes {
    const val LISTINGS = "listings"
    const val DETAIL = "detail/{offerId}"
    const val MAP = "map"
    const val IMPORT_CALCULATOR = "import-calculator"
    const val SOURCE_HEALTH = "source-health"
    fun detail(offerId: String) = "detail/$offerId"
}

@Composable
fun AutkaApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.LISTINGS) {
        composable(Routes.LISTINGS) {
            ListingsRoute(
                onOfferClick = { id -> navController.navigate(Routes.detail(id)) },
                onMapClick = { navController.navigate(Routes.MAP) },
                onImportCalculatorClick = { navController.navigate(Routes.IMPORT_CALCULATOR) },
                onSourceHealthClick = { navController.navigate(Routes.SOURCE_HEALTH) },
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("offerId") { type = NavType.StringType }),
        ) {
            OfferDetailRoute(onBack = { navController.popBackStack() })
        }
        composable(Routes.MAP) {
            MapRoute(
                onBack = { navController.popBackStack() },
                onOfferClick = { id -> navController.navigate(Routes.detail(id)) },
            )
        }
        composable(Routes.IMPORT_CALCULATOR) {
            ImportCalculatorRoute(onBack = { navController.popBackStack() })
        }
        composable(Routes.SOURCE_HEALTH) {
            SourceHealthRoute(onBack = { navController.popBackStack() })
        }
    }
}
