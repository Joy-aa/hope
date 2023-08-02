import os
import cv2
import pydicom
import argparse
import nibabel as nib
import numpy as np
from nibabel import nifti1
from nibabel.viewers import OrthoSlicer3D
from matplotlib import pylab as plt

def get_pixels_hu(dcm):  #获取图像的CT值
    image = dcm.pixel_array
    image[image < 0] = 0

    intercept = dcm.RescaleIntercept
    slope = dcm.RescaleSlope

    image = slope * image
    image += int(intercept)

    return image

def norm_img(img):   #16位图像转换为8位图像进行显示
    max_v = img.max()
    min_v = img.min()
    img = (img - min_v) / (max_v - min_v)
    return img * 255

def main_dicom(sourcePath):
    dcm = pydicom.read_file(sourcePath)
    patient_pixel = get_pixels_hu(dcm)
    img_norm = norm_img(patient_pixel)
    return img_norm

def main_nii(sourcePath):
    '''
    图像维度：第1,2,3维度（0维统计使用维度个数）保留给空间维度x，y，z，而第四维度留给时间维度t。第5,6,7维度可供用户其他使用。
    '''
    img = nib.load(sourcePath)
    if len(img.dataobj.shape) == 3:
        width,height,queue = img.dataobj.shape
        img_arr_1 = img.dataobj[:,:,queue / 2] #选择3D图像的切面
        img_arr_2 = img.dataobj[width / 2,:,:]
        img_arr_3 = img.dataobj[:,height / 2,:]
    else:
        width, height, queue, t = img.dataobj.shape
        img_arr_1 = img.dataobj[width / 2,:,:,1]  #选择3D图像的切面
        img_arr_2 = img.dataobj[:,height / 2,:,1]
        img_arr_3 = img.dataobj[:,:,queue / 2,1]
    img_arr_1 = np.rot90(img_arr_1, 1)  #逆时针旋转90°
    img_arr_2 = np.rot90(img_arr_2, 1)
    img_arr_3 = np.rot90(img_arr_3, 1)
    return img_arr_1, img_arr_2, img_arr_3

def main_mrxs(sourcePath):
    slide = openslide.OpenSlide(sourcePath)
    thumbnail = slide.get_thumbnail((3000, 3000))
    return thumbnail


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='manual to this script')
    parser.add_argument('sourcePath', type=str, default=None)
    parser.add_argument('destPath', type=str, default=None)
    args = parser.parse_args()

    sourcePath = args.sourcePath
    destPath = args.destPath


    extension = sourcePath.split('.')[-1]
    fileName = sourcePath.split('/')[-1].split('.')[-2]

    if extension == "dcm":
        img = main_dicom(sourcePath)
        cv2.imwrite(os.path.join(destPath, fileName + '.png'), img)
    elif extension == "mrxs":
        print(sourcePath + "\n" + destPath + "\n" + fileName)
        img = main_mrxs(sourcePath)
        img.save(os.path.join(destPath, fileName + '.mrxs.png'))
    elif extension == "gz" or extension == "nii":
        if extension == 'gz':
            fileName = sourcePath.split('/')[-1].split('.')[-3]
        img_1,img_2,img_3 = main_nii(sourcePath)
        # print(os.path.join(destPath, fileName + '.jpg'))
        width_1,height_1 = img_1.shape
        width_2,height_2 = img_2.shape
        width_3,height_3 = img_3.shape

        plt.figure(figsize=(height_1,width_1),dpi=1)
        plt.gca().xaxis.set_major_locator(plt.NullLocator())
        plt.gca().yaxis.set_major_locator(plt.NullLocator())
        plt.subplots_adjust(top = 1, bottom = 0, right = 1, left = 0, hspace = 0, wspace = 0)
        plt.margins(0,0)
        plt.imshow(img_1, cmap=plt.cm.gray)
        plt.savefig(os.path.join(destPath, fileName + '-1.png'),pad_inches=0)  # dpi=100 和上文相对应 pixel尺寸/dpi=inch尺寸

        plt.figure(figsize=(height_2,width_2),dpi=1)
        plt.gca().xaxis.set_major_locator(plt.NullLocator())
        plt.gca().yaxis.set_major_locator(plt.NullLocator())
        plt.subplots_adjust(top = 1, bottom = 0, right = 1, left = 0, hspace = 0, wspace = 0)
        plt.margins(0,0)
        plt.imshow(img_2, cmap=plt.cm.gray)
        plt.savefig(os.path.join(destPath, fileName + '-2.png'),pad_inches=0)  # dpi=100 和上文相对应 pixel尺寸/dpi=inch尺寸

        plt.figure(figsize=(height_3,width_3),dpi=1)
        plt.gca().xaxis.set_major_locator(plt.NullLocator())
        plt.gca().yaxis.set_major_locator(plt.NullLocator())
        plt.subplots_adjust(top = 1, bottom = 0, right = 1, left = 0, hspace = 0, wspace = 0)
        plt.margins(0,0)
        plt.imshow(img_3, cmap=plt.cm.gray)
        plt.savefig(os.path.join(destPath, fileName + '-3.png'),pad_inches=0)  # dpi=100 和上文相对应 pixel尺寸/dpi=inch尺寸
        #cv2.imwrite(os.path.join(destPath, fileName + '-1.png'), img_1)
        #cv2.imwrite(os.path.join(destPath, fileName + '-2.png'), img_2)
        #cv2.imwrite(os.path.join(destPath, fileName + '-3.png'), img_3)
    elif extension == "analyze":               #analyze文件格式未知
        pass
    else:
        pass
