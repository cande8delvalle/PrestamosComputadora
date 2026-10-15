package com.ien.prestamoscomputadoras.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ien.prestamoscomputadoras.ui.screens.HistorialPrestamosScreen
import com.ien.prestamoscomputadoras.ui.screens.HomeScreen
import com.ien.prestamoscomputadoras.ui.screens.LoginScreen
import com.ien.prestamoscomputadoras.ui.screens.RegisterScreen
import com.ien.prestamoscomputadoras.ui.screens.RegistrarAlumnoScreen
import com.ien.prestamoscomputadoras.ui.screens.RegistrarComputadoraScreen
import com.ien.prestamoscomputadoras.ui.screens.RegistrarDevolucionScreen
import com.ien.prestamoscomputadoras.ui.screens.RegistrarPrestamoScreen
import com.ien.prestamoscomputadoras.ui.screens.RolesPermisosScreen
import com.ien.prestamoscomputadoras.ui.screens.SplashScreen
import com.ien.prestamoscomputadoras.ui.screens.WelcomeScreen
import com.ien.prestamoscomputadoras.viewmodel.HistorialPrestamosViewModel
import com.ien.prestamoscomputadoras.viewmodel.HomeViewModel
import com.ien.prestamoscomputadoras.viewmodel.LoginViewModel
import com.ien.prestamoscomputadoras.viewmodel.RegisterViewModel
import com.ien.prestamoscomputadoras.viewmodel.RegistrarAlumnoViewModel
import com.ien.prestamoscomputadoras.viewmodel.RegistrarComputadoraViewModel
import com.ien.prestamoscomputadoras.viewmodel.RegistrarDevolucionViewModel
import com.ien.prestamoscomputadoras.viewmodel.RegistrarPrestamoViewModel
import com.ien.prestamoscomputadoras.viewmodel.RolesPermisosViewModel
import com.ien.prestamoscomputadoras.util.SesionActual
import com.ien.prestamoscomputadoras.viewmodel.DestinoInicial
import com.ien.prestamoscomputadoras.viewmodel.SplashViewModel

/** Rutas de la app. */
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Welcome : Screen("welcome")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Home : Screen("home")
    data object RegistrarAlumno : Screen("registrar_alumno")
    data object NuevoPrestamo : Screen("nuevo_prestamo")
    data object RegistrarDevolucion : Screen("registrar_devolucion")
    data object RegistrarComputadora : Screen("registrar_computadora")
    data object HistorialPrestamos : Screen("historial_prestamos")
    data object RolesPermisos : Screen("roles_permisos")
}

/**
 * Grafo de navegación de la app: Splash → Welcome → Login / Register → Home → pantallas
 * secundarias (Registrar Alumno, Nuevo Préstamo, Registrar Devolución, ...).
 *
 * Cada `composable()` crea su ViewModel con `viewModel()`, así queda asociado a esa entrada
 * del back stack y se destruye cuando la pantalla sale de la pila.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // Si una consulta confirmó que la sesión venció (ni refrescando el token se pudo), se lleva
    // al login con el aviso. En las pantallas sin sesión (Splash, Bienvenida, Login, Registro)
    // no hace falta: se descarta el aviso.
    val sesionVencida by SesionActual.sesionVencida.collectAsState()
    LaunchedEffect(sesionVencida) {
        if (!sesionVencida) return@LaunchedEffect
        val rutasSinSesion = setOf(Screen.Splash.route, Screen.Welcome.route, Screen.Login.route, Screen.Register.route)
        if (navController.currentDestination?.route in rutasSinSesion) {
            SesionActual.consumirAvisoSesionVencida()
            return@LaunchedEffect
        }
        SesionActual.usuarioId = null
        SesionActual.nombreUsuario = null
        navController.navigate(Screen.Login.route) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }

    NavHost(navController = navController, startDestination = Screen.Splash.route) {

        composable(Screen.Splash.route) {
            val splashViewModel: SplashViewModel = viewModel(factory = SplashViewModel.Factory)
            var animacionTerminada by remember { mutableStateOf(false) }
            SplashScreen(onSplashFinished = { animacionTerminada = true })

            // Se navega cuando terminaron las dos cosas: la animación y la verificación de la
            // sesión guardada. Splash sale de la pila: "atrás" desde el destino cierra la app.
            val destino = splashViewModel.destino
            LaunchedEffect(animacionTerminada, destino) {
                if (!animacionTerminada || destino == null) return@LaunchedEffect
                val ruta = if (destino == DestinoInicial.HOME) Screen.Home.route else Screen.Welcome.route
                navController.navigate(ruta) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
        }

        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onCrearCuentaClick = { navController.navigate(Screen.Register.route) },
                onIniciarSesionClick = { navController.navigate(Screen.Login.route) },
            )
        }

        composable(Screen.Login.route) {
            val loginViewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory)
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
            val registerViewModel: RegisterViewModel = viewModel(factory = RegisterViewModel.Factory)
            RegisterScreen(
                viewModel = registerViewModel,
                onIniciarSesionClick = { navController.irALoginDesdeRegister() },
                onCuentaCreada = { navController.irALoginDesdeRegister() },
            )
        }

        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
            HomeScreen(
                viewModel = homeViewModel,
                // launchSingleTop: un doble toque no apila dos veces la misma pantalla.
                onRegistrarAlumnoClick = {
                    navController.navigate(Screen.RegistrarAlumno.route) { launchSingleTop = true }
                },
                onRegistrarComputadoraClick = {
                    navController.navigate(Screen.RegistrarComputadora.route) { launchSingleTop = true }
                },
                onNuevoPrestamoClick = {
                    navController.navigate(Screen.NuevoPrestamo.route) { launchSingleTop = true }
                },
                onRegistrarDevolucionClick = {
                    navController.navigate(Screen.RegistrarDevolucion.route) { launchSingleTop = true }
                },
                onHistorialClick = {
                    navController.navigate(Screen.HistorialPrestamos.route) { launchSingleTop = true }
                },
                // Solo el Admin ve este acceso en Home (y la pantalla vuelve a verificarlo).
                onRolesPermisosClick = {
                    navController.navigate(Screen.RolesPermisos.route) { launchSingleTop = true }
                },
                onLogoutClick = {
                    homeViewModel.cerrarSesion()
                    // Se limpia todo el back stack: después de cerrar sesión no se puede volver a Home.
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.RegistrarAlumno.route) {
            val registrarAlumnoViewModel: RegistrarAlumnoViewModel =
                viewModel(factory = RegistrarAlumnoViewModel.Factory)
            RegistrarAlumnoScreen(
                viewModel = registrarAlumnoViewModel,
                onVolverClick = { navController.volverAHome() },
                onVolverAlInicioClick = { navController.volverAHome() },
            )
        }

        composable(Screen.RegistrarComputadora.route) {
            val registrarComputadoraViewModel: RegistrarComputadoraViewModel =
                viewModel(factory = RegistrarComputadoraViewModel.Factory)
            RegistrarComputadoraScreen(
                viewModel = registrarComputadoraViewModel,
                onVolverClick = { navController.volverAHome() },
                onVolverAlInicioClick = { navController.volverAHome() },
            )
        }

        composable(Screen.NuevoPrestamo.route) {
            val registrarPrestamoViewModel: RegistrarPrestamoViewModel =
                viewModel(factory = RegistrarPrestamoViewModel.Factory)
            RegistrarPrestamoScreen(
                viewModel = registrarPrestamoViewModel,
                onVolverClick = { navController.volverAHome() },
                onVolverAlInicioClick = { navController.volverAHome() },
            )
        }

        composable(Screen.RegistrarDevolucion.route) {
            val registrarDevolucionViewModel: RegistrarDevolucionViewModel =
                viewModel(factory = RegistrarDevolucionViewModel.Factory)
            RegistrarDevolucionScreen(
                viewModel = registrarDevolucionViewModel,
                // La flecha solo llega acá desde la etapa de escaneo: desde la confirmación
                // la pantalla vuelve primero a la cámara (estado del ViewModel).
                onVolverClick = { navController.volverAHome() },
                onVolverAlInicioClick = { navController.volverAHome() },
            )
        }

        composable(Screen.HistorialPrestamos.route) {
            val historialPrestamosViewModel: HistorialPrestamosViewModel =
                viewModel(factory = HistorialPrestamosViewModel.Factory)
            HistorialPrestamosScreen(
                viewModel = historialPrestamosViewModel,
                onVolverClick = { navController.volverAHome() },
            )
        }

        composable(Screen.RolesPermisos.route) {
            val rolesPermisosViewModel: RolesPermisosViewModel =
                viewModel(factory = RolesPermisosViewModel.Factory)
            RolesPermisosScreen(
                viewModel = rolesPermisosViewModel,
                onVolverClick = { navController.volverAHome() },
            )
        }
    }
}

/**
 * Vuelve a Home sacando de la pila todo lo que está encima. Se usa `popBackStack(route)` en
 * vez de `popBackStack()` porque es idempotente: si se toca "volver" dos veces rápido, el
 * segundo llamado no encuentra nada que sacar y no se lleva a Home de la pila.
 */
private fun NavHostController.volverAHome() {
    popBackStack(Screen.Home.route, inclusive = false)
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
