
package com.transportpro;

import java.util.List;

public class TestEmployees {

    public static void main(String[] args) {
        try {
            EmployeeDAO dao = new EmployeeDAO();
            List<String[]> employees = dao.getAllEmployees();

            System.out.println("Nombre d'employes : " + employees.size());

            for (String[] employee : employees) {
                System.out.println(
                    employee[0] + " | " +
                    employee[1] + " | " +
                    employee[2] + " | " +
                    employee[3] + " | " +
                    employee[4] + " | " +
                    employee[5]
                );
            }

        } catch (Exception e) {
            System.out.println("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }
}

