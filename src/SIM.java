

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class SIM {

	PrintWriter writer;

	private int bitsWritten;

	private String INPUTFILE;
	private String OUTPUTFILE;

	private List<String> finalBuffer;
	private static String combined = "";

	private List<String> original;

	private static List<String> dictionary;

	public static void main(String [] args){
		new SIM(args[0]);
	}

	public SIM(String x){
		bitsWritten = 0;
		try {
			if (x.equals("1")) {
				INPUTFILE = "original.txt";
				OUTPUTFILE = "cout.txt";
				original = Files.readAllLines(Paths.get(INPUTFILE));
				writer = new PrintWriter(OUTPUTFILE);
				compress();
			} else if (x.equals("2")) {
				INPUTFILE = "compressed.txt";
				OUTPUTFILE = "dout.txt";
				original = Files.readAllLines(Paths.get(INPUTFILE));
				writer = new PrintWriter(OUTPUTFILE);
				finalBuffer = new ArrayList<>();
				decompress();
				int newlinecounter = 0;
				for (String buf : finalBuffer) {
					writer.print(buf);
					if (newlinecounter != finalBuffer.size()-1){
						writer.println();
					}

					newlinecounter++;
				}
				writer.close();
			}

		} catch (IOException e) {
			writer.close();
			System.err.println("failed to open input file");
			e.printStackTrace();
		}
	}

	private void compress(){
		createDictionary();
		List<String> encoding = new ArrayList<>();
		List<Integer> encodingSize = new ArrayList<>();

		for (int i = 0; i < original.size(); i++){
			String s = original.get(i);

			if (/*dictionary.contains(s) &&*/ i != 0 && original.get(i - 1).equals(s)){ //RLE
				int counter = 0;
				for (int j = i + 1; i < original.size(); j++){
					if (counter == 3){
						break;
					}
					else if(original.get(j).equals(s)){
						counter++;
					} else { break; }
				}
				i += counter;
				writeEncoding("000"
						+ String.format("%2s",Integer.toBinaryString(counter)).replace(' ','0'));
				encoding.clear();
				encodingSize.clear();
				continue;
			} else if (dictionary.contains(s)){ //101 + dictionary index
				writeEncoding("101"
						+ String.format("%3s",Integer.toBinaryString(dictionary.indexOf(s))).replace(' ','0'));
				encoding.clear();
				encodingSize.clear();
				continue;
			}


			encoding.add(bitmask(s));
			encoding.add(oneBitMismatch(s));
			encoding.add(twoBitMismatch(s));
			encoding.add(doubleMismatch(s));
			encoding.add("110" + s);

			encodingSize.add(encoding.get(0).length());
			encodingSize.add(encoding.get(1).length());
			encodingSize.add(encoding.get(2).length());
			encodingSize.add(encoding.get(3).length());
			encodingSize.add(s.length() + 3);

			int index = getIndexOfSmallestValue(encodingSize);
			writeEncoding(encoding.get(index));

			encoding.clear();
			encodingSize.clear();

		}
		//padding
		for (;bitsWritten != 32; bitsWritten++) {
			writer.print("1");
		}
		writer.println("\nxxxx");
		for (int i = 0; i < 7; i++) {
			writer.println(dictionary.get(i));
		}
		writer.print(dictionary.get(7));
		writer.close();
	}

	private void writeEncoding(String s){
		//writer.println(s);
		System.out.println(s);
		for (int i = 0; i < s.length(); i++){
			if (bitsWritten == 32){
				writer.println();
				bitsWritten = 0;
			}
			writer.print(s.charAt(i));
			bitsWritten++;
		}
	}

	private void createDictionary(){
		dictionary = new ArrayList<>();

		List<String> listUnique = new ArrayList<>();
		int [] count = new int[(new LinkedHashSet<>(original).size())];
		for (String t : original){
			if (listUnique.contains(t)){
				int index = listUnique.indexOf(t);
				count[index]++;
			} else {
				listUnique.add(t);
				count[listUnique.indexOf(t)]++;
			}
		}

		for(int i = 0; i < 8; i++){
			int max = -1;
			int index = -1;
			for (int j = 0; j < count.length; j++){
				if (count[j] > max){
					max = count[j];
					index = j;
				}
			}
			dictionary.add(listUnique.get(index));
			count[index] = 0;
		}

	}

	private int getIndexOfSmallestValue(List<Integer> sizes){
		int min = 36;
		int index = 0;
		for(int i = 0; i < sizes.size(); i++){
			if (sizes.get(i) < min){
				min = sizes.get(i);
				index = i;
			}
		}
		return index;
	}

	private int getMismatchLocation(String a, String b, int mismatchSize){ //returns index of mismatch, -1 if more than 1 mismatch
		int index = -1;
		for (int i = 0; i < a.length(); i++){
			if (a.charAt(i) != b.charAt(i)){
				index = i;
				break;
			}
		}
		//TODO: what if at end
		for (int i = index + mismatchSize; i < a.length(); i++){
			if (a.charAt(i) != b.charAt(i)){
				return -1;
			}
		}
		return index;
	}

	private String bitmask(String s){
		String bitmask = "";
		int index = -1;
		int dictionaryIndex = 0;
		for (String t : dictionary) {
			index = getMismatchLocation(s, t, 4);
			if (index != -1){
				break;
			}
			dictionaryIndex++;
		}
		if (index == -1 || index + 2 >= s.length()) { //minor change
			return "000000000000000000000000000000000000";
		}
		//calc bitmask
		//try {
			for (int i = index; i < index + 4; i++) {
				if (s.charAt(i) != dictionary.get(dictionaryIndex).charAt(i)) {
					bitmask += "1";
				} else {
					bitmask += "0";
				}
			}
		//} catch(IndexOutOfBoundsException e ){ //minor change
		//	return "000000000000000000000000000000000000";
		//}
		return "001"
				+ String.format("%5s",Integer.toBinaryString(index)).replace(' ','0')
				+ bitmask
				+ String.format("%3s",Integer.toBinaryString(dictionaryIndex)).replace(' ','0');
	}

	private String oneBitMismatch(String s){
		int index = -1;
		int dictionaryIndex = 0;
		for (String t : dictionary) {
			index = getMismatchLocation(s, t, 1);
			if (index != -1){
				break;
			}
			dictionaryIndex++;
		}

		if (index == -1){
			return "000000000000000000000000000000000000";
		}

		return "010" + String.format("%5s",Integer.toBinaryString(index)).replace(' ','0')
				+ String.format("%3s",Integer.toBinaryString(dictionaryIndex)).replace(' ','0');
	}

	private String twoBitMismatch(String s){
		int index = -1;
		int dictionaryIndex = 0;
		for (String t : dictionary) {
			index = getMismatchLocation(s, t, 2);
			if (index != -1){
				break;
			}
			dictionaryIndex++;
		}
		if (index == -1){
			return "000000000000000000000000000000000000";
		}

		return "011" + String.format("%5s",Integer.toBinaryString(index)).replace(' ','0')
				+ String.format("%3s",Integer.toBinaryString(dictionaryIndex)).replace(' ','0');
	}

	private String doubleMismatch(String a){
		int index1 = -1;
		int index2 = -1;
		int dictionaryIndex = 0;
		int numMismatch = 0;
		boolean success = false;
		for (String b : dictionary){
			numMismatch = 0;
			index1 = -1;
			index2 = -1;
			for(int i = 0; i < a.length(); i++){
				if (a.charAt(i) != b.charAt(i)){ //found first mismatch
					numMismatch++;
					for (int j = i + 1; j < a.length(); j++){
						if (a.charAt(j) != b.charAt(j)){ //found second mismatch
							numMismatch++;
							for(int k = j + 1; k < a.length(); k++ ){
								if (a.charAt(k) != b.charAt(k)){ //found third mismatch -- return
									numMismatch++;
									break;
								}
								if (k == a.length() - 1){
									success = true;
								}
							}
							index2 = j;
							break;
						}
					}
					index1 = i;
					break;
				}
			}

			if(index1 != -1 && index2 != -1 && numMismatch != 3){
				success = true; //minor change
				break;
			}
			dictionaryIndex++;
		}

		if (index1 == -1 || index2 == -1 ){
			return "000000000000000000000000000000000000";
		}
		if (success) {
			return "100" + String.format("%5s", Integer.toBinaryString(index1)).replace(' ', '0')
					+ String.format("%5s", Integer.toBinaryString(index2)).replace(' ', '0')
					+ String.format("%3s", Integer.toBinaryString(dictionaryIndex)).replace(' ', '0');
		} else {
			return "000000000000000000000000000000000000";
		}
	}



	private void decompress(){
		getDictionary();

		String opcode;
		String arg1;
		String arg2;
		String arg3;



		for (int i = 0; i < original.size() - 9; i++){
			combined += original.get(i);
		}

		for (int i = 0; i < combined.length(); ){
			opcode = combined.substring(i,i+3);
			i += 3;
			if (opcode.equals("000")){
				arg1 = combined.substring(i,i+2);
				i += 2;
				for (int j = 0; j < Integer.parseInt(arg1,2) + 1; j++) {
					finalBuffer.add(finalBuffer.get(finalBuffer.size()-1));
				}
			} else if (opcode.equals("001")) {
				arg1 = combined.substring(i,i+5); //location
				i += 5;
				arg2 = combined.substring(i,i+4); //bitmask
				i += 4;
				arg3 = combined.substring(i,i+3); //dictionary index
				i += 3;
				String temp = dictionary.get(Integer.parseInt(arg3,2));
				int location = Integer.parseInt(arg1,2);
				String xor = temp.substring(0,location);
				if (arg2.charAt(0) == '1'){
					xor +=  (temp.charAt(location) == '1' ? '0' : '1');
				} else { xor += temp.charAt(location); }

				if (arg2.charAt(1) == '1'){
					xor += (temp.charAt(location + 1) == '1' ? '0' : '1');
				} else { xor += temp.charAt(location+1); }

				if (arg2.charAt(2) == '1'){
					xor += (temp.charAt(location + 2) == '1' ? '0' : '1');
				} else { xor += temp.charAt(location+2); }

				if (arg2.charAt(3) == '1'){
					xor += (temp.charAt(location + 3) == '1' ? '0' : '1');
				} else { xor += temp.charAt(location+3); }

				xor += temp.substring(location+4);

				finalBuffer.add(xor);

			} else if (opcode.equals("010")) {
				arg1 = combined.substring(i,i+5);
				i += 5;
				arg2 = combined.substring(i,i+3);
				i += 3;
				int location = Integer.parseInt(arg1,2);
				String temp = dictionary.get(Integer.parseInt(arg2,2)).substring(0,
						location);
				if (dictionary.get(Integer.parseInt(arg2,2)).charAt(location) == '1'){
					temp += '0';
				} else { temp += '1'; }
				temp += dictionary.get(Integer.parseInt(arg2,2)).substring(location+1);
				finalBuffer.add(temp);
			} else if (opcode.equals("011")) {
				arg1 = combined.substring(i,i+5);
				i += 5;
				arg2 = combined.substring(i,i+3);
				i += 3;
				int location = Integer.parseInt(arg1,2);
				String temp = dictionary.get(Integer.parseInt(arg2,2)).substring(0,location);
				if (dictionary.get(Integer.parseInt(arg2,2)).charAt(location) == '1'){
					temp += '0';
				} else { temp += '1'; }

				if (dictionary.get(Integer.parseInt(arg2,2)).charAt(location+1) == '1'){
					temp += '0';
				} else { temp += '1'; }

				temp += dictionary.get(Integer.parseInt(arg2,2)).substring(location+2);

				finalBuffer.add(temp);
			} else if (opcode.equals("100")) {
				arg1 = combined.substring(i,i+5);
				i += 5;
				arg2 = combined.substring(i,i+5);
				i += 5;
				arg3 = combined.substring(i,i+3);
				i += 3;

				int mismatch1 = Integer.parseInt(arg1,2);
				int mismatch2 = Integer.parseInt(arg2,2);
				int dictionaryIndex = Integer.parseInt(arg3,2);

				String temp = dictionary.get(dictionaryIndex).substring(0,mismatch1);

				if (dictionary.get(dictionaryIndex).charAt(mismatch1) == '1'){
					temp += '0';
				} else {temp += '1';}

				temp += dictionary.get(dictionaryIndex).substring(mismatch1+1,mismatch2);

				if (dictionary.get(dictionaryIndex).charAt(mismatch2) == '1'){
					temp += '0';
				} else {temp += '1';}

				temp += dictionary.get(dictionaryIndex).substring(mismatch2+1);
				finalBuffer.add(temp);

			} else if (opcode.equals("101")) {
				arg1 = combined.substring(i,i+3);
				i += 3;
				finalBuffer.add(dictionary.get(Integer.parseInt(arg1,2)));
			} else if (opcode.equals("110")) {
				arg1 = combined.substring(i,i+32);
				i += 32;
				finalBuffer.add(arg1);
			} else {
				break;
			}
		}


	}

	private void getDictionary(){
		dictionary = new ArrayList<>();
		for (int i = original.size() - 8; i < original.size(); i++){
			dictionary.add(original.get(i));
		}
	}




}
