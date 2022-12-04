package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;
import java.util.Random;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;


    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        int menuprint = 0;

        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            if (menuprint != 0) {
                System.out.println();
            }
            System.out.println("*** Please enter one of the following commands ***");
            System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
            System.out.println("> create_caregiver <username> <password>");
            System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
            System.out.println("> login_caregiver <username> <password>");
            System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
            System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
            System.out.println("> upload_availability <date>");
            System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
            System.out.println("> add_doses <vaccine> <number>");
            System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
            System.out.println("> logout");  // TODO: implement logout (Part 2)
            System.out.println("> quit");
            System.out.println();
            System.out.print("> ");
            menuprint++;
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        String username = tokens[1];
        String password = tokens[2];
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        // check if the username has been taken already
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the Patient
        try {
            currentPatient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to Patient information to our database
            currentPatient.saveToDB();
            System.out.println("Created Patient user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create Patient user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patient WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            currentCaregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentCaregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        if (currentPatient != null || currentPatient != null) {
            System.out.println("This patient is already logged in.");
            return;
        }

        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        // check 1: if someone is not logged-in, they need to log in first
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        String date = tokens[1];

        if (tokens.length != 2) {
            System.out.println("Login failed.");
            return;
        }


        String findAvailabilities = "SELECT a.username, v.Name, v.Doses FROM Availabilities a, Vaccines v WHERE Time = ? ORDER By a.username";
        try {
            Date d = Date.valueOf(date);
            PreparedStatement statement = con.prepareStatement(findAvailabilities);
            statement.setString(1, tokens[1]);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String username = resultSet.getString("username");
                String Name = resultSet.getString("Name");
                int Doses = resultSet.getInt("Doses");
                System.out.println("Current Availability:" + username + ", " + Name + ", " + Doses);
            }
            return;
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void reserve(String[] tokens) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }

        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        if (currentPatient == null) {
            System.out.println("Please login as a patient first!");
            return;
        }

        String date = tokens[1];
        String inputvaccinename = tokens[2];

        Random random = new Random();
        int x = random.nextInt(900) + 100;

        String Appointmentfinder = "SELECT a.Time, a.username, v.Name, v.Doses FROM Availabilities a, Vaccines v WHERE Time = ? and Name = ? and Doses > 0";
        try {
            Date d = Date.valueOf(date);
            PreparedStatement statement = con.prepareStatement(Appointmentfinder);
            statement.setString(1, tokens[1]);
            statement.setString(2, tokens[2]);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String time = resultSet.getString("time");
                String username = resultSet.getString("username");
                if (username == null) {
                    System.out.println("No Caregiver is available!");
                    return;
                }
                String Name = resultSet.getString("Name");
                if (tokens[2] == null) {
                    System.out.println("Not enough available doses!");
                    return;
                }
                System.out.println("Appointment ID: " + x + " Caregiver username: " + username);
                String addAppointment = "INSERT INTO Appointment " + "VALUES ('" + x + "', '" + time + "', '" + currentPatient.getUsername() + "', '" + username + "', '" + Name +"')";
                try {
                    PreparedStatement statement2 = con.prepareStatement(addAppointment);
                    statement2.executeUpdate();
                } catch (SQLException e) {
                    System.out.println("Please try again!");
                    e.printStackTrace();
                }
                String dropAvailabilities = "DELETE FROM Availabilities WHERE Username = '" + username + "'";
                try {
                    PreparedStatement statement3 = con.prepareStatement(dropAvailabilities);
                    statement3.executeUpdate();
                } catch (SQLException e) {
                    System.out.println("Please try again!");
                    e.printStackTrace();
                }
                Vaccine vaccine = null;
                try {
                    vaccine = new Vaccine.VaccineGetter(inputvaccinename).get();
                } catch (SQLException e) {
                    System.out.println("Error occurred when adding doses");
                    e.printStackTrace();
                }
                try {
                    vaccine.decreaseAvailableDoses(1);
                } catch (SQLException e) {
                    System.out.println("Please try again!");
                    e.printStackTrace();
                }
            }
            return;
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }


    }

    private static void uploadAvailability(String[] tokens) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];

        String testAvailabilities = "SELECT app.ID, app.Carename FROM Appointment app WHERE Time = ? and Carename = '" + currentCaregiver.getUsername() + "'";
        try {
            PreparedStatement statement3 = con.prepareStatement(testAvailabilities);
            statement3.setString(1, tokens[1]);
            ResultSet resultSet = statement3.executeQuery();
            while (resultSet.next()) {
                String username = resultSet.getString("Carename");
                System.out.println( username + " already has a appointment on " + date);
                return;
            }
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        // check 1: if someone is not logged-in, they need to log in first
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }


        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }

        String ID = tokens[1];

        String findAvailabilities = "SELECT * FROM Appointment app, Vaccines v WHERE ID = ? ";
        try {
            PreparedStatement statement = con.prepareStatement(findAvailabilities);
            statement.setString(1, tokens[1]);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String Carename = resultSet.getString("Carename");
                String vaccines = resultSet.getString("vaccines");
                String Time = resultSet.getString("Time");
                String Patname = resultSet.getString("Patname");
                if (currentPatient != null) {
                    String patient = currentPatient.getUsername();
                    if (Patname.equals(patient)) {
                        String dropAvailabilities = "DELETE FROM Appointment WHERE ID = '" + ID + "'";
                        try {
                            PreparedStatement statement3 = con.prepareStatement(dropAvailabilities);
                            statement3.executeUpdate();
                        } catch (SQLException e) {
                            System.out.println("Please try again!");
                            e.printStackTrace();
                        }
                        String addAppointment = "INSERT INTO Availabilities " + "VALUES ('" + Time + "', '" + Carename + "')";
                        try {
                            PreparedStatement statement2 = con.prepareStatement(addAppointment);
                            statement2.executeUpdate();
                            System.out.println("Your Appointment" + ID + "has been successfully cancel");
                        } catch (SQLException e) {
                            System.out.println("Please try again!");
                            e.printStackTrace();
                        }

                        Vaccine vaccine = null;
                        try {
                            vaccine = new Vaccine.VaccineGetter(vaccines).get();
                        } catch (SQLException e) {
                            System.out.println("Error occurred when adding doses");
                            e.printStackTrace();
                        }
                        try {
                            vaccine.increaseAvailableDoses(1);
                        } catch (SQLException e) {
                            System.out.println("Please try again!");
                            e.printStackTrace();
                        }  finally {
                        cm.closeConnection();
                    }
                    }
                    return;
                } else {
                    System.out.println("This is not your appointment, please log in as the Caregiver or Patient to cancel this Appointment");
                }
                    if (currentCaregiver != null) {
                        String cargiver = currentCaregiver.getUsername();
                        if (Carename.equals(cargiver)) {
                            String dropAvailabilities = "DELETE FROM Appointment WHERE ID = '" + ID + "'";
                            try {
                                PreparedStatement statement3 = con.prepareStatement(dropAvailabilities);
                                statement3.executeUpdate();
                            } catch (SQLException e) {
                                System.out.println("Please try again!");
                                e.printStackTrace();
                            }
                            String addAppointment = "INSERT INTO Availabilities " + "VALUES ('" + Time + "', '" + Carename + "')";
                            try {
                                PreparedStatement statement2 = con.prepareStatement(addAppointment);
                                statement2.executeUpdate();
                                System.out.println("Your Appointment" + ID + "has been successfully cancel");
                            } catch (SQLException e) {
                                System.out.println("Please try again!");
                                e.printStackTrace();
                            }

                            Vaccine vaccine = null;
                            try {
                                vaccine = new Vaccine.VaccineGetter(vaccines).get();
                            } catch (SQLException e) {
                                System.out.println("Error occurred when adding doses");
                                e.printStackTrace();
                            }
                            try {
                                vaccine.increaseAvailableDoses(1);
                            } catch (SQLException e) {
                                System.out.println("Please try again!");
                                e.printStackTrace();
                            } finally {
                            cm.closeConnection();
                        }
                        }
                        return;
                    } else {
                        System.out.println("This is not your appointment, please log in as the Caregiver or Patient to cancel this Appointment");
                    }
            }
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        }
        return;
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        // check 1: if someone is not logged-in, they need to log in first
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }

        if (currentCaregiver != null) {
            String printCAppointments = "SELECT app.ID , app.vaccines, app.Time, app.Patname FROM Appointment app Where app.Carename = '" + currentCaregiver.getUsername() + "' ORDER By app.ID";
            try {
                PreparedStatement statement1 = con.prepareStatement(printCAppointments);
                ResultSet resultSet = statement1.executeQuery();
                while (resultSet.next()) {
                    int ID = resultSet.getInt("ID");
                    String vaccines = resultSet.getString("vaccines");
                    String Time = resultSet.getString("Time");
                    String patname = resultSet.getString("Patname");
                    System.out.println("Current Appointment: " + ID + ", " + vaccines + ", " + Time + "," + patname);
                }
            } catch (SQLException e) {
                System.out.println("Please try again!");
                e.printStackTrace();
            } finally {
                cm.closeConnection();
            }
        }

        if (currentPatient != null) {
            String printPAppointments = "SELECT app.ID , app.vaccines, app.Time, app.Carename FROM Appointment app WHERE app.Patname = '" + currentPatient.getUsername() + "' ORDER By app.ID";
            try {
                PreparedStatement statement = con.prepareStatement(printPAppointments);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    String ID = resultSet.getString("ID");
                    String vaccines = resultSet.getString("vaccines");
                    String Time = resultSet.getString("Time");
                    String Carename = resultSet.getString("Carename");
                    System.out.println("Current Appointment: " + ID + ", " + vaccines + ", " + Time + "," + Carename);
                }
            } catch (SQLException e) {
                System.out.println("Please try again!");
                e.printStackTrace();
            } finally {
                cm.closeConnection();
            }
        }
    }

    private static void logout(String[] tokens) {

        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        if (currentCaregiver != null) {
            currentCaregiver = null;
            System.out.println("Successfully logged out!");
            return;
        }
        if (currentPatient != null) {
            currentPatient = null;
            System.out.println("Successfully logged out!");
            return;
        }
    }
}
