package com.ien.prestamoscomputadoras.data.repository

import com.ien.prestamoscomputadoras.data.entity.Administrador
import com.ien.prestamoscomputadoras.data.entity.Rol
import com.ien.prestamoscomputadoras.viewmodel.FakeAdministradorDao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdministradorRepositoryTest {

    private val dao = FakeAdministradorDao()
    private val repo = AdministradorRepository(dao)

    private fun cuenta(usuario: String, idRol: Long = Rol.ID_PERSONAL) = Administrador(
        nombre = "N",
        apellido = "A",
        nombreUsuario = usuario,
        dni = "1",
        email = "$usuario@ien.edu.ar",
        contrasenaHash = "x",
        idRol = idRol,
    )

    @Test
    fun primeraCuenta_esAdmin_lasSiguientesPersonal() = runTest {
        repo.registrar(cuenta("primero"))
        repo.registrar(cuenta("segundo"))
        repo.registrar(cuenta("tercero"))

        assertEquals(
            listOf(Rol.ID_ADMIN, Rol.ID_PERSONAL, Rol.ID_PERSONAL),
            dao.administradores.map { it.idRol },
        )
    }

    @Test
    fun conAdminExistente_nuncaSeRegistraOtroAdmin_aunqueSePida() = runTest {
        repo.registrar(cuenta("admin"))

        // Aunque el objeto venga con rol Admin, se registra como Personal Administrativo.
        repo.registrar(cuenta("intruso", idRol = Rol.ID_ADMIN))

        assertEquals(1, dao.administradores.count { it.idRol == Rol.ID_ADMIN })
        assertEquals(Rol.ID_PERSONAL, dao.administradores.single { it.nombreUsuario == "intruso" }.idRol)
    }

    @Test
    fun primeraCuenta_esAdmin_aunqueSePidaPersonal() = runTest {
        repo.registrar(cuenta("primero", idRol = Rol.ID_PERSONAL))
        assertEquals(Rol.ID_ADMIN, dao.administradores.single().idRol)
    }

    @Test
    fun personalIlimitado() = runTest {
        repo.registrar(cuenta("admin"))
        (1..20).forEach { repo.registrar(cuenta("p$it")) }

        assertEquals(1, dao.administradores.count { it.idRol == Rol.ID_ADMIN })
        assertEquals(20, dao.administradores.count { it.idRol == Rol.ID_PERSONAL })
        assertTrue(dao.administradores.first().idRol == Rol.ID_ADMIN)
    }
}
