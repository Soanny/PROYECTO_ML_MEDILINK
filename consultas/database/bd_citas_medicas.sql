CREATE DATABASE IF NOT EXISTS bd_citas_medicas;
USE bd_citas_medicas;
SET FOREIGN_KEY_CHECKS=0;

-- =========================
-- TABLA ROLES
-- =========================
DROP TABLE IF EXISTS roles;
CREATE TABLE roles (
    id_rol INT AUTO_INCREMENT PRIMARY KEY,
    nombre_rol VARCHAR(50) NOT NULL UNIQUE,
    descripcion_rol VARCHAR(150)
);

INSERT INTO roles (nombre_rol, descripcion_rol) VALUES
('Administrador','Acceso total al sistema'),
('Secretaria','Gestiona pacientes, citas y pagos'),
('Doctor','Atiende citas y registra historial clínico'),
('Paciente','Reserva citas médicas');

-- =========================
-- TABLA USUARIOS
-- =========================
DROP TABLE IF EXISTS usuarios;
CREATE TABLE usuarios (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    id_rol INT NOT NULL,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    dni VARCHAR(15) UNIQUE,
    celular VARCHAR(20),
    direccion VARCHAR(150),
    correo VARCHAR(100) NOT NULL UNIQUE,
    contraseña VARCHAR(255) NOT NULL,
    estado ENUM('Activo','Inactivo') DEFAULT 'Activo',
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_rol) REFERENCES roles(id_rol)
);

INSERT INTO usuarios (id_rol, nombres, apellidos, dni, celular, direccion, correo, contraseña) VALUES
(1,'Admin','Principal','00000001','999999999','Oficina central','admin@gmail.com','123456'),
(2,'María','López','11111111','987654321','Av. Perú 123','secretaria1@gmail.com','123456'),
(2,'Sofía','García','11111112','987654322','Av. Perú 124','secretaria2@gmail.com','123456'),
(3,'Carlos','Ramírez','22222222','955555555','Av. Salud 456','doctor1@gmail.com','123456'),
(3,'Ana','Torres','22222223','955555556','Av. Salud 457','doctor2@gmail.com','123456'),
(3,'Luis','Mendoza','22222224','955555557','Av. Salud 458','doctor3@gmail.com','123456'),
(3,'Roberto','Navarro','22222225','955555558','Av. Salud 459','doctor4@gmail.com','123456'),  -- NUEVO DOCTOR
(4,'Pedro','Gonzales','33333333','944444441','Jr. Lima 100','paciente1@gmail.com','123456'),
(4,'Lucía','Ramirez','33333334','944444442','Jr. Lima 101','paciente2@gmail.com','123456'),
(4,'Jorge','Pérez','33333335','944444443','Jr. Lima 102','paciente3@gmail.com','123456'),
(4,'Marta','Flores','33333336','944444444','Jr. Lima 103','paciente4@gmail.com','123456');

-- =========================
-- TABLA SECRETARIAS
-- =========================
DROP TABLE IF EXISTS secretarias;
CREATE TABLE secretarias (
    id_secretaria INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL UNIQUE,
    fecha_ingreso DATE,
    turno ENUM('Mañana','Tarde','Noche') DEFAULT 'Mañana',
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

INSERT INTO secretarias (id_usuario, fecha_ingreso, turno) VALUES
(2,'2026-05-01','Mañana'),
(3,'2026-05-02','Tarde');

-- =========================
-- TABLA PACIENTES
-- =========================
DROP TABLE IF EXISTS pacientes;
CREATE TABLE pacientes (
    id_paciente INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT UNIQUE,
    fecha_nacimiento DATE,
    genero ENUM('Masculino','Femenino','Otro'),
    grupo_sanguineo VARCHAR(10),
    alergias TEXT,
    contacto_emergencia VARCHAR(100),
    celular_emergencia VARCHAR(20),
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

INSERT INTO pacientes (id_usuario, fecha_nacimiento, genero, grupo_sanguineo, alergias, contacto_emergencia, celular_emergencia) VALUES
(8,'2000-04-15','Masculino','O+','Ninguna','Ana Torres','933333333'),
(9,'1995-07-20','Femenino','A+','Penicilina','Luis Pérez','933333334'),
(10,'1980-02-10','Masculino','B+','Polen','Carla Mendoza','933333335'),
(11,'2003-11-05','Femenino','AB+','Ninguna','Jorge López','933333336');

-- =========================
-- TABLA ESPECIALIDADES
-- =========================
DROP TABLE IF EXISTS especialidades;
CREATE TABLE especialidades (
    id_especialidad INT AUTO_INCREMENT PRIMARY KEY,
    nombre_especialidad VARCHAR(100) NOT NULL UNIQUE,
    descripcion_especialidad TEXT,
    estado ENUM('Activo','Inactivo') DEFAULT 'Activo'
);

INSERT INTO especialidades (nombre_especialidad, descripcion_especialidad) VALUES
('Medicina General','Atención médica general'),
('Pediatría','Atención médica para niños'),
('Cardiología','Especialidad del corazón'),
('Dermatología','Especialidad de la piel');

-- =========================
-- TABLA CONSULTORIOS
-- =========================
DROP TABLE IF EXISTS consultorios;
CREATE TABLE consultorios (
    id_consultorio INT AUTO_INCREMENT PRIMARY KEY,
    nombre_consultorio VARCHAR(100) NOT NULL,
    ubicacion VARCHAR(150),
    piso VARCHAR(20),
    estado ENUM('Disponible','Ocupado','Mantenimiento') DEFAULT 'Disponible'
);

INSERT INTO consultorios (nombre_consultorio, ubicacion, piso, estado) VALUES
('Consultorio 101','Área A','1','Disponible'),
('Consultorio 102','Área A','1','Disponible'),
('Consultorio 201','Área B','2','Disponible'),
('Consultorio 202','Área B','2','Disponible');

-- =========================
-- TABLA DOCTORES
-- =========================
DROP TABLE IF EXISTS doctores;
CREATE TABLE doctores (
    id_doctor INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL UNIQUE,
    id_especialidad INT NOT NULL,
    id_consultorio INT NOT NULL,
    nro_colegiatura VARCHAR(50) NOT NULL UNIQUE,
    precio_consulta DECIMAL(10,2) DEFAULT 0.00,
    estado ENUM('Activo','Inactivo') DEFAULT 'Activo',
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario),
    FOREIGN KEY (id_especialidad) REFERENCES especialidades(id_especialidad),
    FOREIGN KEY (id_consultorio) REFERENCES consultorios(id_consultorio)
);

INSERT INTO doctores (id_usuario, id_especialidad, id_consultorio, nro_colegiatura, precio_consulta) VALUES
(4,1,1,'CMP-12345',50.00),   -- Carlos Ramírez - Medicina General
(5,2,2,'CMP-12340',69.50),   -- Ana Torres - Pediatría
(6,3,3,'CMP-12347',70.00),   -- Luis Mendoza - Cardiología
(7,4,4,'CMP-12348',80.00);   -- Roberto Navarro - Dermatología (NUEVO)

-- =========================
-- TABLA HORARIOS
-- =========================
DROP TABLE IF EXISTS horarios;
CREATE TABLE horarios (
    id_horario INT AUTO_INCREMENT PRIMARY KEY,
    id_doctor INT NOT NULL,
    dia_semana ENUM('Lunes','Martes','Miércoles','Jueves','Viernes','Sábado','Domingo') NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    estado ENUM('Activo','Inactivo') DEFAULT 'Activo',
    FOREIGN KEY (id_doctor) REFERENCES doctores(id_doctor)
);

INSERT INTO horarios (id_doctor, dia_semana, hora_inicio, hora_fin, estado) VALUES
(1,'Lunes','08:00:00','12:00:00','Activo'),
(2,'Martes','09:00:00','13:00:00','Activo'),
(3,'Miércoles','10:00:00','14:00:00','Activo'),
(4,'Jueves','08:00:00','12:00:00','Activo');

-- =========================
-- TABLA CITAS
-- =========================
DROP TABLE IF EXISTS citas;
CREATE TABLE citas (
    id_cita INT AUTO_INCREMENT PRIMARY KEY,
    id_paciente INT NOT NULL,
    id_doctor INT NOT NULL,
    id_consultorio INT NOT NULL,
    fecha_cita DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    motivo TEXT,
    estado ENUM('Pendiente','Confirmada','Atendida','Cancelada') DEFAULT 'Pendiente',
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_paciente) REFERENCES pacientes(id_paciente),
    FOREIGN KEY (id_doctor) REFERENCES doctores(id_doctor),
    FOREIGN KEY (id_consultorio) REFERENCES consultorios(id_consultorio)
);

INSERT INTO citas (id_paciente, id_doctor, id_consultorio, fecha_cita, hora_inicio, hora_fin, motivo, estado) VALUES
(1,1,1,'2026-05-20','09:00:00','09:30:00','Dolor de cabeza','Confirmada'),
(2,2,2,'2026-05-21','10:00:00','10:30:00','Chequeo general','Pendiente'),
(3,3,3,'2026-05-22','11:00:00','11:30:00','Revisión cardíaca','Confirmada'),
(4,4,4,'2026-05-23','08:30:00','09:00:00','Problemas de piel','Pendiente');

-- =========================
-- TABLA PAGOS
-- =========================
DROP TABLE IF EXISTS pagos;
CREATE TABLE pagos (
    id_pago INT AUTO_INCREMENT PRIMARY KEY,
    id_cita INT NOT NULL,
    monto DECIMAL(10,2) NOT NULL,
    metodo_pago ENUM('Efectivo','Yape','Plin','PayPal','Tarjeta','Transferencia','BanBif') NOT NULL,
    estado_pago ENUM('Pendiente','Pagado','Anulado') DEFAULT 'Pendiente',
    codigo_operacion VARCHAR(100),
    fecha_pago TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_cita) REFERENCES citas(id_cita)
);

INSERT INTO pagos (id_cita, monto, metodo_pago, estado_pago, codigo_operacion) VALUES
(1,50.00,'Yape','Pagado','YP-001'),
(2,60.00,'Plin','Pendiente','PL-002'),
(3,70.00,'PayPal','Pagado','PP-003'),
(4,80.00,'Efectivo','Pendiente','EF-004');

-- =========================
-- TABLA HISTORIAL CLÍNICO
-- =========================
DROP TABLE IF EXISTS historial_clinico;
CREATE TABLE historial_clinico (
    id_historial INT AUTO_INCREMENT PRIMARY KEY,
    id_cita INT NOT NULL,
    id_paciente INT NOT NULL,
    id_doctor INT NOT NULL,
    diagnostico TEXT NOT NULL,
    tratamiento TEXT,
    receta TEXT,
    observaciones TEXT,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_cita) REFERENCES citas(id_cita),
    FOREIGN KEY (id_paciente) REFERENCES pacientes(id_paciente),
    FOREIGN KEY (id_doctor) REFERENCES doctores(id_doctor)
);

INSERT INTO historial_clinico (id_cita, id_paciente, id_doctor, diagnostico, tratamiento, receta, observaciones) VALUES
(1,1,1,'Cefalea','Reposo e hidratación','Paracetamol','Control en 7 días'),
(2,2,2,'Chequeo general','Revisión física','Ninguna','Sin observaciones'),
(3,3,3,'Cardiopatía leve','Ejercicios y dieta','Aspirina','Seguir control mensual'),
(4,4,4,'Dermatitis','Crema tópica','Hidrocortisona','Revisar en 10 días');

ALTER TABLE pagos
ADD COLUMN observacion TEXT;

ALTER TABLE pagos
ADD COLUMN tipo_comprobante
ENUM('Recibo');

SET FOREIGN_KEY_CHECKS=1;
