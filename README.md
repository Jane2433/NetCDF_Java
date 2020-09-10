## NetCdf 简介
使用 Panoply 软件打开 nc 文件，可以看到 nc 文件的基本信息：

![ncInfo](https://github.com/augustuZzl/NetCDF_Java/raw/master/imgs/20200910094806.png)

- 维(dimension)
	可以理解为数学中的向量，或者坐标轴，例如此 nc 文件就有 3 个维：经度、纬度、时间。他的温度数据就体现在这 3 个维上。
- 变量(variables)
    对应着真实的数据，tmin 变量是 float 类型的数据，它的数据分布在 year、lat、lon 这三个维度上，他的物理单位是摄氏度。
    他的数据拿到我们程序中来表示就是一个三维数组：float\[year]\[lat]\[lon]，比如其中的一个数据可以描述为：2020 年 5 月 1 日 东经 120°，北纬 36° 处的温度是 25 ℃。
- 属性(attribute)
    由属性名和属性值组成，用来描述信息。分为整个 nc 文件的全局属性和某个变量的局部属性。
    
可以直接在 panoply 中双击 tmin 变量来绘制该数据，通过这种方式可以大致的了解数据，但这种绘制效果不够精细，我们通过 ArcMap 来绘制：
【多维工具 -> 创建netcdf栅格图层】

![nc栅格图层](https://github.com/augustuZzl/NetCDF_Java/raw/master/imgs/20200910101423.png)

这时我们就可以将栅格导出 ASCII 文来看看 nc 文件的真正内容了。

【转换工具 -> 由栅格转出 -> 栅格转ASCII】

![ncAscII数据](https://github.com/augustuZzl/NetCDF_Java/raw/master/imgs/20200910102710.png)

- xllcorner、yllcorner：左下角的坐标（-180、-90）
- cellsize：像元的大小，此处表示每个像元占 1.5°，这也是栅格图层看起来颗粒感满满的原因，我们后面会对它进行插值，提高他的分辨率，让它过度起来更平滑
- ncols、nrows：数据的行列数，此 nc 文件是 240 \* 120，这就可以理解成 x 方向 240 个像元，y 方向 120 个像元
- NODATA_value：无数据的值，表示没有温度数据就用 -9999 代替，因为此 nc 文件是温度数据，所以没有出现无数据的情况。
                但如果 nc 文件表示的是海浪、洋流这些海洋数据，那么陆地上就可以用 -9999 来表示了。

![nc像元绘制原理](https://github.com/augustuZzl/NetCDF_Java/raw/master/imgs/20200910104611.png)

程序读取的是每个像元中心点的坐标及对应的数据值。

## 读取 NetCdf

`compile("edu.ucar:netcdf:4.2.20")`

读取数据

```java
NetcdfFile ncFile = null;
try {
    ncFile = NetcdfDataset.open(ncPath);
} catch(Exception e) {
    e.printStackTrace();
} finally {
  try {
      if (ncFile != null) {
          ncFile.close();
      }
  } catch (IOException e) {
      e.printStackTrace();
  }
}
```

```java
// 读取维度信息
List<Dimension> dimensions = ncFile.getDimensions();
Dimension lonDim = dimensions.get(0);
Dimension latDim = dimensions.get(1);

Dimension yearDim = ncFile.findDimension("year");
```

```java
// 全局属性
List<Attribute> globalAttributes = ncFile.getGlobalAttributes();
Attribute gAttribute1 = globalAttributes.get(0);

Attribute gAttribute2 = ncFile.findGlobalAttribute("Source_Software");
```

```java
 // 读取变量信息
List<Variable> variables = ncFile.getVariables();
Variable lonVar = variables.get(0);
Variable latVar = variables.get(1);
Variable yearVar = variables.get(2);

Variable tminVar = ncFile.findVariable("tmin");
```

```java
// 读取变量属性
List<Attribute> tminAttributes = tminVar.getAttributes();
Attribute tminAttribute2 = tminVar.findAttribute("units");
```

```java
// 读取变量维度
List<Dimension> tminDimensions = tminVar.getDimensions();
int yearIndex = tminVar.findDimensionIndex("year");
Dimension tminYearDimension = tminDimensions.get(yearIndex);
```

```java
// 读取一维数据
double[] lat = (double[]) latVar.read().copyTo1DJavaArray();
// 读取多维数据
float[][][] tmin = (float[][][]) tminVar.read().copyToNDJavaArray();
```

```java
int[] origin = new int[] { 0, 0, 0 };
int[] size = new int[] { yearDim.getLength(), latDim.getLength(), lonDim.getLength() };

float[][][] tminRange = (float[][][]) tminVar.read(origin, size).copyToNDJavaArray();
```

## 创建 NetCdf 文件

```java
NetcdfFileWriteable newNcFile = null;
try {
    newNcFile = NetcdfFileWriteable.createNew(ncPath.substring(0, ncPath.length() - 3) + "_new.nc");

    Dimension lonDim = newNcFile.addDimension("lon", 240);
    Dimension latDim = newNcFile.addDimension("lat", 120);

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

    //Array tminValue = Array.factory(int.class, new int[] { 4 }, new int[] { 6, 4, 99, 103 });
    //newNcFile.addVariableAttribute("tmin", "missing_value", tminValue);

    newNcFile.create();

} catch (IOException e) {
    e.printStackTrace();
} finally {
    try {
        if (newNcFile != null) {
            newNcFile.close();
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
}
```

![ncNewInfo](https://github.com/augustuZzl/NetCDF_Java/raw/master/imgs/20200910115117.png)

## 写入 NetCdf 文件

```java
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
```

## 双线性插值

![双线性插值](https://github.com/augustuZzl/NetCDF_Java/raw/master/imgs/Bilinear_interpolation.png)

```java
public static float linearInterpolate(double x, double x1, double x2, double x1value, double x2value) {
    return (float) (((x2 - x) / (x2 - x1) * x1value) + ((x - x1) / (x2 - x1) * x2value));
}
```

```java
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
```

```java
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
```

查看插值后的效果，发现颜色过度更加平滑了。

![插值nc栅格图层](https://github.com/augustuZzl/NetCDF_Java/raw/master/imgs/20200910161135.png)
