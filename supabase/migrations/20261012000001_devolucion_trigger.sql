create or replace function public.completar_devolucion()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  if new.estado = 'DEVUELTO' and old.estado = 'ACTIVO' then
    new.fecha_devolucion := now();
    new.id_usuario_devolucion := public.mi_perfil_id();
  end if;
  return new;
end $$;

create trigger prestamo_completar_devolucion
before update on public.prestamo
for each row execute function public.completar_devolucion();
