import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class Application {
    private Date initializationDate = new Date();
    private DatagramSocket socket;
    private int port;
    private InetAddress IP;
    private HashMap<Integer,LinkedBlockingDeque<Animal>> clients = new HashMap<>();
    private Gson gson = new Gson();

    public void application(String s) {
        Runtime.getRuntime().addShutdownHook(new Thread(){
            public void run(){
                try {
                    close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        });
        int server_port = Integer.parseInt(s);
        String[] command;
        try {
            socket = new DatagramSocket(server_port);
            byte[] b = new byte[1024];
            DatagramPacket rec = new DatagramPacket(b, b.length);
            while (true) {
                try {
                    socket.receive(rec);
                    byte[] data = rec.getData();
                    port = rec.getPort();
                    IP = rec.getAddress();
                    clients.putIfAbsent(port,new LinkedBlockingDeque<>());
                    command = new String(data, 0, rec.getLength()).split(" ");
                    String check = "";
                    if(command[0].equals("remove")||command[0].equals("add")||command[0].equals("add_if_max")){
                        check = command[1];
                    }
                    String element = check;
                    switch (command[0]) {
                        case ("import"):
                            myImport(rec);
                            break;
                        case ("remove_last"):
                            new Thread(()->{if (clients.get(port).size() > 0)
                                clients.get(port).removeLast();
                            else
                                sender("Unable to execute command! Collection is empty.");}).start();
                            break;
                        case ("remove"):
                            new Thread(()->myRemove(element)).start();
                            break;
                        case ("add"):
                            new Thread(()->myAdd(element)).start();
                            break;
                        case ("add_if_max"):
                            new Thread(()->addIfMax(element)).start();
                            break;
                        case ("show"):
                            new Thread(this::show).start();
                            break;
                        case ("info"):
                            new Thread(this::info).start();
                            break;
                        case ("load"):
                            new Thread(() -> {
                                try {
                                    readFile("Client" + port + ".xml");
                                } catch (FileNotFoundException e) {
                                    sender("File does not imported!");
                                }
                            }).start();
                            break;
                        case ("exit"):
                            new Thread(()->{
                                try{
                                    close();
                                }catch(IOException e){
                                    e.printStackTrace();
                                }
                            }).start();
                            break;
                        case("save"):
                            new Thread(()->{
                                try{
                                    close();
                                }catch (IOException e){
                                    e.printStackTrace();
                                }
                            }).start();
                            break;
                        default:
                            new Thread(()->sender("The command entered is incorrect!\n" + "Example of valid command:\n" +
                                    "remove_last\n" + "remove {element}\n" + "add {element}\n" + "show\n" + "info\n" + "load\n" +
                                    "add_if_max {element}\n" + "import\n"+"save\n")).start();
                    }
                } catch (JsonSyntaxException e) {
                    new Thread(()->sender("The element is invalid format. Expected format XML!")).start();
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * method readFile
     * reads the objects from file
     * and fills with them LinkedList
     * the file format must be XML
     * and objects's format mast be XML.
     * @param s
     */
    private void readFile(String s) throws FileNotFoundException{
        String[] results;
        String n = "";
        int si = 0;
        double l = 0.0;
        clients.get(port).clear();
        File file = new File(s);
        Scanner scanner = new Scanner(file);
        try {
            while (scanner.hasNext()) {
                String line = scanner.nextLine().trim();
                if (line.contains("<Animal ")) {
                    results = line.split(" ");
                    for(String res: results){
                        if(res.contains("=")){
                            switch (res.split("=")[0]) {
                                case "name":
                                    n = res.split("=")[1].substring(1,res.split("=")[1].length()-1);
                                    break;
                                case "location":
                                    l = Double.parseDouble(res.split("=")[1].substring(1,res.split("=")[1].length()-3));
                                    break;
                                case "size":
                                    si = Integer.parseInt(res.split("=")[1].substring(1,res.split("=")[1].length()-1));
                                    break;
                            }
                        }
                    }
                    LinkedBlockingDeque<Animal> ls = clients.get(port);
                    ls.add(new Animal(n, si, l));
                    clients.replace(port,clients.get(port),ls);
                    ls = null;
                }
            }
        }catch(Exception e){
            new Thread(()->sender("The file contains invalid data. Expected format XML!")).start();
        }
    }

    /**
     * method show
     * outputs in standard output stream
     * an LinkedList of homes
     * can be cosed from terminal by this way: show
     */
    private void show(){
        ArrayList<Animal> list = new ArrayList<>(clients.get(port));
        Collections.sort(list);
        clients.replace(port,clients.get(port),new LinkedBlockingDeque<>(list));
        sender(clients.get(port).toString());
    }

    /**
     * method info
     * outputs in standard output stream
     * collection class
     * element class
     * number of elements
     * initialization date
     * can be cosed from terminal by this way: info
     */
    private void info(){
        String so = "Collection type: "+clients.get(port).getClass()+"\n"+"Element's class type: "+Animal.class+"\n"+
                "Date initialization: "+initializationDate+"\n"+"Collection size: "+clients.get(port).size();
        sender(so);
    }

    /**
     * method close
     * stops program execution
     * and saves a state of LinkedList in file
     * file should be format XML
     * elements that will appeared it file will have format json
     * method can be cosed from terminal by this way: close
     * @throws IOException
     */
    private void close() throws IOException {
            String outputFile = "Client"+port+".xml";
            BufferedWriter pw = new BufferedWriter(new FileWriter(outputFile));
            pw.write("<?xml version= \"1.0\"encoding=\"utf-8\"?>\n");
            pw.write("<Animal>\n");
            clients.get(port).stream().forEach((x) -> {
                        try {
                            pw.write("<Animal name=" + "\""+x.getName()+"\"" + " size=" +"\""+x.getSize()+"\"" +
                                    " location=" + "\""+x.getLocation()+"\"" + "/>\n");
                        }catch (IOException writeEx){ System.err.println("Output EXCEPTION during writing"); }
                    });
            pw.write("<Animal/>");
            pw.close();
    }

    /**
     * method addIfMax
     * add it's parameter at LinkedList of Animal
     * if it's value exceeds the value of the largest element of this collection
     * elements format must be json
     * can be cosed from terminal by this way: addIfMax
     * @param element
     */
    private void addIfMax(String element){
        try {
            Animal animal = gson.fromJson(element, Animal.class);
            if (animal.getName() != null && animal.getSize() > 0 && animal.getLocation() > 0.0) {
                List<Animal> list = new ArrayList<>(clients.get(port));
                list.add(animal);
                Collections.sort(list);
                if (animal.equals(list.get(list.size() - 1))) {
                    clients.replace(port, clients.get(port), new LinkedBlockingDeque<>(list));
                }
            } else
                sender("Element's value is incorrect! Example of value:" + "\n" +
                        "{name:string value,size:integer value,location:double value}");
        }catch (com.google.gson.JsonSyntaxException jx){
            sender("The strings describing the objects must be in the form of json!");
        }
    }

    /**
     * method myAdd
     * add it's parameter at LinkedList of Animal
     * elements format must be json
     * can be cosed from terminal by this way: add {element}
     * @param element
     */
    private void myAdd(String element){
        try {
            Animal animal = gson.fromJson(element, Animal.class);
            if (animal.getName() != null) {
                clients.get(port).add(animal);
            } else
                sender("Enter Animal's name, please");
        }catch (com.google.gson.JsonSyntaxException jx){
            sender("The strings describing the objects must be in the form of json!");
        }
    }

    private void myRemove(String element){
        Animal animal = null;
        try {
            animal = gson.fromJson(element, Animal.class);
        }catch (com.google.gson.JsonSyntaxException ex) {
            sender("The strings describing the objects must be in the form of json!");
        }
        if (clients.get(port).contains(animal)){
            clients.get(port).remove(animal);
        }else {
            sender("This element is not in collection!");
        }
    }

    private void myImport(DatagramPacket rec) {
        try {
            BufferedWriter buwr = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("Client" + port + ".xml")));
            while (true) {
                socket.receive(rec);
                byte[] in = rec.getData();
                String ins = new String(in, 0, rec.getLength());
                if (ins.equals("stop") || ins.equals("Exception")) break;
                buwr.write(ins);
                buwr.newLine();
            }
            buwr.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    synchronized private void sender(String str){
        try {
            byte[]  nb = str.getBytes();
            DatagramPacket out = new DatagramPacket(nb, nb.length, IP, port);
            socket.send(out);
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
