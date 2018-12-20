package net.localguru.sfz2multisample;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.util.StringTokenizer;
import javax.sound.sampled.*;

public class Sfz2Multisample {
    public static void main(String args[]) {
        Sfz2Multisample main = new Sfz2Multisample();

        String sfz_name = args[0];
        String multi_name = sfz_name.replace("sfz", "multisample");
        String sfz = main.loadSfz(sfz_name);
        System.out.println(sfz_name);

//        System.out.println( sfz );

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<multisample name=\"" + sfz_name + "\">\n" +
                "<generator>Bitwig Studio</generator>\n" +
                "<category></category>\n" +
                "<creator>SFZ2Multisample</creator>\n" +
                "<description/>\n" +
                "<keywords/>\n" +
                "<layer name=\"Default\">\n";

        StringTokenizer tok = new StringTokenizer(sfz, "<[^>]*>");
        List<String> sampleNames = new ArrayList<String>();
        String default_path = "";

        String mode = "";
        while (tok.hasMoreTokens()) {
            String res = tok.nextToken();
            if ("region".equals(res) || "group".equals(res) || "control".equals(res)) {
                mode = res;
                continue;
            }
            String[] tmp = res.trim().split(" ");
            Map<String, String> attributes = new HashMap<String, String>();
            String sample = "";
            boolean s = false;
            String key = "";
            for (String t : tmp) {
                if (t.indexOf("=") == -1 && s) {
                    sample += t + " ";
                    continue;
                }
//                System.out.println( "T: " + t );
                String[] data = t.split("=");
                if (data[0].trim().equals("")) continue;
                key = data[0];
                if (!data[0].trim().equals("sample") && !data[0].trim().equals("default_path")) {
                    attributes.put(data[0].trim(), data[1].trim());
                } else {
                    sample = data[1].trim() + " ";
                    s = true;
                }
            }
            if ("default_path".equals(key)) {
                default_path = sample.trim();
                System.out.println("DP: " + default_path);
            }
            if (default_path != null && !"".equals(default_path)) {
                sample = default_path + sample;
            }
            sample = sample.trim().replace("\\", "/");
            if ("sample".equals(key)) {
                attributes.put("sample", sample);
                sampleNames.add(sample);
            }

            if (attributes.get("key") != null) {
                String k = attributes.get("key");
                attributes.put("hikey", k);
                attributes.put("lokey", k);
                attributes.put("pitch_keycenter", k);

            }

            if (mode.equals("region")) {
                if (attributes.get("sample") == null) {
                    attributes.put("sample", sample);
                    sampleNames.add(sample);
                }
                float end = 1000F;
                try {
                    System.out.println("Sample: " + attributes.get("sample"));
                    AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(sample));
                    long len = audioInputStream.getFrameLength();
                    AudioFormat format = audioInputStream.getFormat();
                    float fr = format.getFrameRate();
                    end = len;

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                String[] parts = sample.split("/");

                String root = Sfz2Multisample.map_note_to_midi(attributes.get("pitch_keycenter"));
                String high_key = Sfz2Multisample.map_note_to_midi(attributes.get("hikey"));
                String low_key = Sfz2Multisample.map_note_to_midi(attributes.get("lokey"));
                System.out.println(root + " " + high_key + " " + low_key + " " + attributes);

                xml += "<sample file=\"" + parts[parts.length - 1].trim() + "\" gain=\"0.000\" sample-start=\"0.000\" sample-stop=\"" + end + "\" tune=\"0.0\">\n";
                xml += "<key high=\"" + high_key + "\" low=\"" + low_key + "\" root=\"" + root + "\" track=\"true\"/>\n";
                xml += "<velocity/>\n";
                xml += "<loop/>\n";
                xml += "</sample>\n";
            }
        }
        xml += "</layer>\n";
        xml += "</multisample>\n";

        List<String> sampleNamesSeen = new ArrayList<String>();

        try {
            ZipOutputStream result = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(multi_name)));
            for (String name : sampleNames) {
                if (sampleNamesSeen.contains(name)) {
                    continue;
                }
                sampleNamesSeen.add(name);
                try {
                    BufferedInputStream fis = new BufferedInputStream(new FileInputStream(name), 4096);
                    String[] parts = name.split("/");
                    System.out.println(name);
                    result.putNextEntry(new ZipEntry(parts[parts.length - 1]));
                    int count = 0;
                    byte[] data = new byte[4096];
                    while ((count = fis.read(data, 0, 4096)) != -1) {
                        result.write(data, 0, count);
                    }
                    fis.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            result.putNextEntry(new ZipEntry("multisample.xml"));
            byte[] data = xml.getBytes("UTF-8");

            result.write(data, 0, data.length);


            result.close();
        } catch (Exception ioe) {
            throw new RuntimeException(ioe);
        }


    }

    public String loadSfz(String filename) {
        StringBuilder b = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
            String l = null;
            while ((l = reader.readLine()) != null) {
                String content[] = l.split("//");
                if (content.length > 0) {
                    System.out.println(content[0]);
                    b.append(content[0]);
                    b.append(" ");
                }
            }
            reader.close();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        return b.toString();
    }

    // TODO set up a test module for this. test this function!
    // TODO support negative numbers maybe?
    public static String map_note_to_midi(String root) {
        // todo static generate map
        HashMap<String, Integer> note_to_midi_offset_map = new HashMap<String, Integer>();
        note_to_midi_offset_map.put("a", -3);
        note_to_midi_offset_map.put("a#", -2);
        note_to_midi_offset_map.put("b", -1);
        note_to_midi_offset_map.put("c", 0);
        note_to_midi_offset_map.put("c#", 1);
        note_to_midi_offset_map.put("d", 2);
        note_to_midi_offset_map.put("d#", 3);
        note_to_midi_offset_map.put("e", 4);
        note_to_midi_offset_map.put("f", 5);
        note_to_midi_offset_map.put("f#", 6);
        note_to_midi_offset_map.put("g", 7);
        note_to_midi_offset_map.put("g#", 8);
        if (root.length() == 2) {
            String note = root.substring(0, 1);
            int index = Integer.parseInt(root.substring(1));
            if (note_to_midi_offset_map.containsKey(note)) {
                int x = ((index) * 12) + note_to_midi_offset_map.get(note);
                return "" + x;
            }
        } else if (root.length() == 3) {
            String note = root.substring(0, 2);
            int index = Integer.parseInt(root.substring(2));
            if (note_to_midi_offset_map.containsKey(note)) {
                int x = ((index) * 12) + note_to_midi_offset_map.get(note);
                return "" + x;
            }
        }
        return root;
    }
}
