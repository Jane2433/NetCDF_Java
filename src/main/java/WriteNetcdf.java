import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.Index;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.IOException;

/**
 * @CreateTime: 2020/9/10 11:52
 * @Author: zzl
 * @Description: 写入数据到 nc 文件
 */
public class WriteNetcdf {

    public static void write(String srcNcPath, String targetNcPath) {

        NetcdfFile srcNcFile = null;
        NetcdfFileWriteable targetNcFile = null;
        try {
            srcNcFile = NetcdfDataset.open(srcNcPath);

            double[] lat = (double[]) srcNcFile.findVariable("lat").read().copyTo1DJavaArray();
            double[] lon = (double[]) srcNcFile.findVariable("lon").read().copyTo1DJavaArray();

            int[] origin = new int[] { 0, 0, 0};
            int[] size = new int[] { 1, lat.length, lon.length };
            float[][][] tmin = (float[][][]) srcNcFile.findVariable("tmin").read(origin, size).copyToNDJavaArray();

            targetNcFile = NetcdfFileWriteable.openExisting(targetNcPath);

            ArrayDouble latArray = new ArrayDouble.D1(lat.length);
            for (int i = 0; i < lat.length; i++) {
                latArray.setDouble(i, lat[i]);
            }
            targetNcFile.write("lat", latArray);

            ArrayDouble lonArray = new ArrayDouble.D1(lon.length);
            for (int i = 0; i < lon.length; i++) {
                lonArray.setDouble(i, lon[i]);
            }
            targetNcFile.write("lon", lonArray);

            ArrayFloat tminArray = new ArrayFloat.D2(lat.length, lon.length);
            Index tminIndex = tminArray.getIndex();
            for (int i = 0; i < lat.length; i++) {
                for (int j = 0; j < lon.length; j++) {
                    tminArray.setFloat(tminIndex.set(i, j), tmin[0][i][j]);
                }
            }
            targetNcFile.write("tmin", new int[]{0, 0}, tminArray);

            targetNcFile.flush();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (srcNcFile != null) {
                    srcNcFile.close();
                }
                if (targetNcFile != null) {
                    targetNcFile.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static void main(String[] args) {
        write("D:\\temperature.nc", "D:\\temperature_new.nc");
    }

}
