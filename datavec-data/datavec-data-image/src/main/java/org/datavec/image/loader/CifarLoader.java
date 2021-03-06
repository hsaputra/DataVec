/*
 *  * Copyright 2016 Skymind, Inc.
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 */

package org.datavec.image.loader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.datavec.image.transform.ImageTransform;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.*;
import java.util.*;

/**
 * Reference: Learning Multiple Layers of Features from Tiny Images, Alex Krizhevsky, 2009.
 * Created by nyghtowl on 12/17/15.
 */
public class CifarLoader extends NativeImageLoader implements Serializable {
    public final static int NUM_TRAIN_IMAGES = 50000;
    public final static int NUM_TEST_IMAGES = 10000;
    public final static int NUM_LABELS = 10; // 6000 imgs per class
    public final static int HEIGHT = 32;
    public final static int WIDTH = 32;
    public final static int CHANNELS = 3;
    public final static int BYTEFILELEN = 3073;

    public String dataUrl = "https://www.cs.toronto.edu/~kriz/cifar-10-python.tar.gz"; // used for python version - similar structure to datBin structure
    public String dataFile = "cifar-10-python";
    public static String dataBinUrl = "https://www.cs.toronto.edu/~kriz/cifar-10-binary.tar.gz";
    public static String dataBinFile = "cifar-10-batches-bin";
    protected static String labelFileName = "batches.meta.txt";
    protected static List<String> labels = new ArrayList<>();

    protected static String[] trainFileNames = {"data_batch_1.bin", "data_batch_2.bin", "data_batch_3.bin", "data_batch_4.bin", "data_batch5.bin"};
    protected static String testFileName = "test_batch.bin";

    public static String localDir = "cifar";
    protected static File fullDir = new File(BASE_DIR, localDir);
    protected boolean train = true;
    public static Map<String, String> cifarTrainData = new HashMap<>();
    // Using this in spark to reference where to load data from
    public final static File TRAINPATH = new File(fullDir, "train");
    public final static File TESTPATH = new File(fullDir, FilenameUtils.concat(dataBinFile, testFileName));
    public final static File LABELPATH = new File(fullDir, FilenameUtils.concat(dataBinFile, labelFileName));
    static {
        load();
    }


    public CifarLoader(boolean train){
        this.train = train;
    }

    public CifarLoader(int height, int width, int channels, boolean train){
        super(height, width, channels);
        this.train = train;
        load();
    }
    public CifarLoader(int height, int width, int channels, ImageTransform imgTransform, int normalizeValue, boolean train){
        super(height, width, channels, imgTransform, normalizeValue);
        this.train = train;
        load();
    }

    public CifarLoader(boolean train, String localDir){
        this.localDir = localDir;
        this.fullDir = new File(localDir);
        this.train = train;
        load();
    }

    public CifarLoader(){
        load();
    }

    public CifarLoader(int height, int width, int channels){
        super(height, width, channels);
        load();
    }

    @Override
    public INDArray asRowVector(File f) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public INDArray asRowVector(InputStream inputStream) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public INDArray asMatrix(File f) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public INDArray asMatrix(InputStream inputStream) throws IOException {
        throw new UnsupportedOperationException();
    }

    public static void generateMaps() {
        cifarTrainData.put("filesFilename", new File(dataBinUrl).getName());
        cifarTrainData.put("filesURL", dataBinUrl);
        cifarTrainData.put("filesFilenameUnzipped", dataBinFile);
    }

    private static void defineLabels() {
        try {
            File path = new File(fullDir, FilenameUtils.concat(dataBinFile, labelFileName));
            BufferedReader br = new BufferedReader(new FileReader(path));
            String line;

            while ((line = br.readLine()) != null) {
                labels.add(line);
                // TODO resolve duplicate listing
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void load()  {
        if (!imageFilesExist() && !fullDir.exists()) {
            generateMaps();
            fullDir.mkdir();

            log.info("Downloading {}...", localDir);
            downloadAndUntar(cifarTrainData, fullDir);
        }
        defineLabels();
    }

    public static boolean imageFilesExist(){
        File f = new File(fullDir, FilenameUtils.concat(dataBinFile, testFileName));
        if (!f.exists()) return false;

        for(String name: trainFileNames) {
            f = new File(fullDir, FilenameUtils.concat(dataBinFile, name));
            if (!f.exists()) return false;
        }
        return true;
    }

    public InputStream getInputStream() {
        load();
        InputStream in = null;

        try {
            // Create inputStream
            if(train) {
                Collection<File> subFiles = FileUtils.listFiles(new File(fullDir, dataBinFile), new String[] {"bin"}, true);
                Iterator trainIter = subFiles.iterator();
                in = new SequenceInputStream(new FileInputStream((File) trainIter.next()), new FileInputStream((File) trainIter.next()));
                while (trainIter.hasNext()) {
                    File nextFile = (File) trainIter.next();
                    if(!testFileName.equals(nextFile.getName()))
                        in = new SequenceInputStream(in, new FileInputStream(nextFile));
                }
            }
            else
                in = new FileInputStream(new File(fullDir, FilenameUtils.concat(dataBinFile, testFileName)));
        } catch(Exception e) {
            e.printStackTrace();
        }
        return in;
    }

    public List<String> getLabels(){
        return labels;
    }
}
