import React from 'react';
import { Modal, Button, Badge } from 'flowbite-react';
import { FaListUl, FaCheckCircle, FaTimesCircle } from 'react-icons/fa';

export const ViewCategoryModal = ({ show, onClose, categoryData }) => {
  if (!categoryData) return null;

  return (
    <Modal show={show} onClose={onClose}>
      <Modal.Header>Menu Category Details</Modal.Header>
      <Modal.Body>
        <div className="space-y-4">
          {/* Category Header */}
          <div className="flex items-center space-x-4 p-4 bg-gray-50 rounded-lg">
            <div className="p-3 bg-blue-100 rounded-full">
              <FaListUl className="text-blue-600 text-xl" />
            </div>
            <div>
              <h5 className="text-xl font-bold text-gray-900">{categoryData.name}</h5>
              <div className="mt-2">
                <Badge color={categoryData.active ? "success" : "failure"}>
                  {categoryData.active ? 
                    <div className="flex items-center">
                      <FaCheckCircle className="mr-1" />
                      Active
                    </div> : 
                    <div className="flex items-center">
                      <FaTimesCircle className="mr-1" />
                      Inactive
                    </div>
                  }
                </Badge>
              </div>
            </div>
          </div>

          {/* Description */}
          {categoryData.description && (
            <div className="mt-4">
              <h6 className="text-sm font-semibold text-gray-500 uppercase mb-2">Description</h6>
              <p className="text-gray-700 bg-gray-50 p-3 rounded-md">{categoryData.description}</p>
            </div>
          )}
          
          {/* Additional Details */}
          <div className="mt-4">
            <h6 className="text-sm font-semibold text-gray-500 uppercase mb-2">Additional Details</h6>
            <div className="bg-gray-50 p-4 rounded-md">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <p className="text-sm text-gray-500">Category ID</p>
                  <p className="font-medium">{categoryData.id}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-500">Display Order</p>
                  <p className="font-medium">{categoryData.displayOrder || 'Not set'}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-500">Restaurant</p>
                  <p className="font-medium">{categoryData.restaurantName || 'Unknown'}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-500">Menu Items</p>
                  <p className="font-medium">{categoryData.menuItemCount || '0'}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </Modal.Body>
      <Modal.Footer>
        <Button color="gray" onClick={onClose}>
          Close
        </Button>
      </Modal.Footer>
    </Modal>
  );
};