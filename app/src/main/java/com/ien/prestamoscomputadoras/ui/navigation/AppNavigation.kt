package com.ien.prestamoscomputadoras.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ien.prestamoscomputadoras.ui.screens.HomeScreen
import com.ien.prestamoscomputadoras.ui.screens.LoginScreen
import com.ien.prestamoscomputadoras.ui.screens.RegisterScreen
import com.ien.prestamoscomputadoras.ui.screens.SplashScreen
import com.ien.prestamoscomputadoras.ui.screens.WelcomeScreen
import com.ien.prestamoscomputadoras.viewmodel.HomeViewModel
import com.ien.prestamoscomputadoras.viewmodel.LoginViewModel
import com.ien.prestamoscomputadoras.viewmodel.RegisterViewModel

/** Rutas de la app. */
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Welcome : Screen("welcome")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Home : Screen("home")
}

/**
 * Grafo de navegación de la app: Splash → Welcome → Login / Register → Home.
 *
 * Cada `composable()` crea su ViewModel con `viewModel()`, así queda asociado a esa entrada
 * del back stack y se destruye cuando la pantalla sale de la pila.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Splash.route) {

        composable(Screen.Splash.route) {
            SplashScreen(
                // Splash sale de la pila: "atrás" desde Welcome cierra la app.
                onSplashFinished = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onCrearCuentaClick = { navController.navigate(Screen.Register.route) },
                onIniciarSesionClick = { navController.navigate(Screen.Login.route) },
            )
        }

        composable(Screen.Login.route) {
            val loginViewModel: LoginViewModel = viewModel()
            LoginScreen(
                viewModel = loginViewModel,
                onCrearCuentaClick = { navController.navigate(Screen.Register.route) },
                onLoginExitoso = {
                    // Se vacía toda la pila (Welcome incluida): "atrás" desde Home cierra la
                    // app en vez de volver al login.
                    navController.navigate(Screen.Home.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Register.route) {
            val registerViewModel: RegisterViewModel = viewModel()
            RegisterScreen(
                viewModel = registerViewModel,
                onIniciarSesionClick = { navController.irALoginDesdeRegister() },
                onCuentaCreada = { navController.irALoginDesdeRegister() },
            )
        }

        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = viewModel()
            HomeScreen(
                viewModel = homeViewModel,
                // Las acciones rápidas quedan con sus lambdas vacías por defecto (TODO en HomeScreen).
                onLogoutClick = {
                    // Se limpia todo el back stack: después de cerrar sesión no se puede volver a Home.
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
            )
        }
    }
}

/**
 * Va a Login sacando Register de la pila. Con `launchSingleTop`, si se había llegado a Register
 * desde Login se reutiliza ese Login en vez de apilar uno duplicado.
 */
private fun NavHostController.irALoginDesdeRegister() {
    navigate(Screen.Login.route) {
        popUpTo(Screen.Register.route) { inclusive = true }
        launchSingleTop = true
    }
}
