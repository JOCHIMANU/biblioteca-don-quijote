package com.quijote.biblioteca.controlador;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class LectorForm {

    @NotBlank(message = "Falta el nombre del lector.")
    @Size(max = 255, message = "El nombre es demasiado largo.")
    private String nombre;

    @NotBlank(message = "Falta el domicilio: decid dónde mora el lector.")
    @Size(max = 255, message = "El domicilio es demasiado largo.")
    private String domicilio;

    @NotBlank(message = "Falta el celular.")
    @Pattern(regexp = "^\\+?[0-9 ]{6,15}$",
            message = "El celular solo admite números (y un + al inicio), de seis a quince guarismos.")
    private String celular;

    @NotBlank(message = "Falta el DNI.")
    @Pattern(regexp = "^[0-9]{8}$", message = "El DNI ha de constar de ocho guarismos cabales, ni uno más ni uno menos.")
    private String dni;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDomicilio() {
        return domicilio;
    }

    public void setDomicilio(String domicilio) {
        this.domicilio = domicilio;
    }

    public String getCelular() {
        return celular;
    }

    public void setCelular(String celular) {
        this.celular = celular;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }
}
