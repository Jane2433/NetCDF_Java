import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @CreateTime: 2020/9/10 14:42
 * @Author: zzl
 * @Description: 插值
 */
public class InterpolateNetcdf {

    public static void interpolate(String srcNcPath, String outputNcPath) {
        NetcdfFileWriteable newNcFile = null;
        NetcdfFile srcNcFile = null;
        try {
            srcNcFile = NetcdfDataset.open(srcNcPath);

            double[] lat = (double[]) srcNcFile.findVariable("lat").read().copyTo1DJavaArray();
            double[] lon = (double[]) srcNcFile.findVariable("lon").read().copyTo1DJavaArray();

            float[][] tmin = (float[][]) srcNcFile.findVariable("tmin").read().copyToNDJavaArray();

            // 原始数据分辨率
            double dy = lat[1] - lat[0], dx = lon[1] - lon[0];

            // 创建新经纬度数组，扩大 10 倍
            double[] newLat = new double[lat.length * 10], newLon = new double[lon.length * 10];

            // 新的分辨率为原 1/10
            double newDy = dy / 10, newDx = dx / 10;

            // 新的经纬度填充
            for (int i = 0; i < newLat.length; i++) {
                newLat[i] = lat[0] + i * newDy;
            }
            for (int i = 0; i < newLon.length; i++) {
                newLon[i] = lon[0] + i * newDx;
            }

            float[][] res = bilinerInterpolate(tmin, newLat, newLon, lat, lon, dx, dy);

            newNcFile = NetcdfFileWriteable.createNew(outputNcPath);

            Dimension lonDim = newNcFile.addDimension("lon", 240 * 10);
            Dimension latDim = newNcFile.addDimension("lat", 120 * 10);

            List<Dimension> lonDimensions = Collections.singletonList(lonDim);
            newNcFile.addVariable("lon", DataType.DOUBLE, lonDimensions);
            newNcFile.addVariableAttribute("lon", "standard_name", "longitude");
            newNcFile.addVariableAttribute("lon", "units", "degrees_east");

            newNcFile.addVariable("lat", DataType.DOUBLE, Collections.singletonList(latDim));
            newNcFile.addVariableAttribute("lat", "standard_name", "latitude");
            newNcFile.addVariableAttribute("lat", "units", "degrees_north");

            newNcFile.addVariable("tmin", DataType.FLOAT, Arrays.asList(latDim, lonDim));
            newNcFile.addVariableAttribute("tmin", "coordinates", "lon lat");
            newNcFile.addVariableAttribute("tmin", "units", "Degree");
            newNcFile.addVariableAttribute("tmin", "missing_value", -3.402823E38f);

            newNcFile.create();

            ArrayDouble latArray = new ArrayDouble.D1(newLat.length);
            for (int i = 0; i < newLat.length; i++) {
                latArray.setDouble(i, newLat[i]);
            }
            newNcFile.write("lat", latArray);

            ArrayDouble lonArray = new ArrayDouble.D1(newLon.length);
            for (int i = 0; i < newLon.length; i++) {
                lonArray.setDouble(i, newLon[i]);
            }
            newNcFile.write("lon", lonArray);

            ArrayFloat tminArray = new ArrayFloat.D2(newLat.length, newLon.length);
            Index tminIndex = tminArray.getIndex();
            for (int i = 0; i < newLat.length; i++) {
                for (int j = 0; j < newLon.length; j++) {
                    tminArray.setFloat(tminIndex.set(i, j), res[i][j]);
                }
            }
            newNcFile.write("tmin", tminArray);

            newNcFile.flush();


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (newNcFile != null) {
                    newNcFile.close();
                }
                if (srcNcFile != null) {
                    srcNcFile.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static float[][] bilinerInterpolate(float[][] tmin, double[] newLat, double[] newLon, double[] oldLat, double[] oldLon, double cellSizeX, double cellSizeY) {
        double beginLat = oldLat[0], beginLon = oldLon[0];
        int latSize = oldLat.length - 1, lonSize = oldLon.length - 1;

        float[][] res = new float[newLat.length][newLon.length];
        for (int i = 0; i <  newLat.length; i++) {
            double lat = newLat[i];
            for (int j = 0; j < newLon.length; j++) {
                double lon = newLon[j];

                int x1 = (int) Math.floor((lon - beginLon) / cellSizeX);
                int x2 = x1 + 1;
                if (x2 > lonSize) x2 = lonSize;

                int y1 = (int) Math.floor((lat - beginLat) / cellSizeY);
                int y2 = y1 + 1;
                if (y2 > latSize) y2 = latSize;

                double lon1 = x1 * cellSizeX + beginLon;
                double lon2 = x2 * cellSizeX + beginLon;

                double lat1 = y1 * cellSizeY + beginLat;
                double lat2 = y2 * cellSizeY + beginLat;

                double value1 = tmin[y1][x1];
                double value2 = tmin[y1][x2];
                double value3 = tmin[y2][x1];
                double value4 = tmin[y2][x2];

                float r1 = linearInterpolate(lon, lon1, lon2, value1, value2);
                float r2 = linearInterpolate(lon, lon1, lon2, value3, value4);

                res[i][j] = linearInterpolate(lat, lat1, lat2, r1, r2);
            }
        }
        return res;
    }

    /**
     * 线性插值
     * @param x       插值点坐标
     * @param x1      顶点坐标
     * @param x2      顶点坐标
     * @param x1value 顶点数值
     * @param x2value 顶点数值
     * @return 插值后的数值
     */
    public static float linearInterpolate(double x, double x1, double x2, double x1value, double x2value) {
        return (float) (((x2 - x) / (x2 - x1) * x1value) + ((x - x1) / (x2 - x1) * x2value));
    }

    public static void main(String[] args) {
        interpolate("D:\\temperature_new.nc", "D:\\temperature_new_inter.nc");
    }

}
