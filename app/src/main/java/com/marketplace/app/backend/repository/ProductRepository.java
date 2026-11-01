package com.marketplace.app.backend.repository;

import com.marketplace.app.backend.ApiClient;
import com.marketplace.app.backend.mock.MockBackendService;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.model.Category;
import com.marketplace.app.model.FilterCriteria;
import com.marketplace.app.model.Product;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Rôle : Repository des produits et du catalogue.
 * Entrée : Appels API Retrofit ou fallback transparent sur MockBackendService en mode démo.
 * Sortie : Callbacks unifiés Callback<T>.
 */
public class ProductRepository extends BaseRepository<Product> {

    private final boolean useMock;

    public ProductRepository() {
        this(false);
    }

    public ProductRepository(boolean useMock) {
        this.useMock = useMock;
    }

    @Override
    public void list(Object filter, Callback<List<Product>> callback) {
        FilterCriteria criteria = (filter instanceof FilterCriteria) ? (FilterCriteria) filter : null;
        if (useMock) {
            postSuccess(callback, MockBackendService.getInstance().getProducts(criteria));
            return;
        }

        Map<String, String> query = toQuery(criteria);
        ApiClient.getApi().getProducts(query).enqueue(new retrofit2.Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postSuccess(callback, response.body());
                } else {
                    postError(callback, parseHttpError(response));
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                postError(callback, parseNetworkError(t));
            }
        });
    }

    public void listCategories(Callback<List<Category>> callback) {
        if (useMock) {
            postSuccess(callback, MockBackendService.getInstance().getCategories());
            return;
        }

        ApiClient.getApi().getCategories().enqueue(new retrofit2.Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postSuccess(callback, response.body());
                } else {
                    postError(callback, parseHttpError(response));
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                postError(callback, parseNetworkError(t));
            }
        });
    }

    @Override
    public void getById(String id, Callback<Product> callback) {
        if (useMock) {
            Product p = MockBackendService.getInstance().getProductById(id);
            if (p != null) postSuccess(callback, p);
            else postError(callback, "Produit introuvable : " + id);
            return;
        }

        ApiClient.getApi().getProductById(id).enqueue(new retrofit2.Callback<Product>() {
            @Override
            public void onResponse(Call<Product> call, Response<Product> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postSuccess(callback, response.body());
                } else {
                    postError(callback, parseHttpError(response));
                }
            }

            @Override
            public void onFailure(Call<Product> call, Throwable t) {
                postError(callback, parseNetworkError(t));
            }
        });
    }

    @Override
    public void create(Product item, Callback<Product> callback) {
        if (useMock) {
            postSuccess(callback, MockBackendService.getInstance().createProduct(item));
            return;
        }

        ApiClient.getApi().createProduct(item).enqueue(new retrofit2.Callback<Product>() {
            @Override
            public void onResponse(Call<Product> call, Response<Product> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postSuccess(callback, response.body());
                } else {
                    postError(callback, parseHttpError(response));
                }
            }

            @Override
            public void onFailure(Call<Product> call, Throwable t) {
                postError(callback, parseNetworkError(t));
            }
        });
    }

    @Override
    public void update(Product item, Callback<Product> callback) {
        if (useMock) {
            postSuccess(callback, item);
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("title", item.getTitle());
        body.put("price", item.getPrice());
        body.put("stock", item.getStock());
        ApiClient.getApi().updateProduct(item.getId(), body).enqueue(new retrofit2.Callback<Product>() {
            @Override
            public void onResponse(Call<Product> call, Response<Product> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postSuccess(callback, response.body());
                } else {
                    postError(callback, parseHttpError(response));
                }
            }

            @Override
            public void onFailure(Call<Product> call, Throwable t) {
                postError(callback, parseNetworkError(t));
            }
        });
    }

    public void updateModel3d(String productId, String modelUrl, Callback<Boolean> callback) {
        if (useMock) {
            boolean ok = MockBackendService.getInstance().updateProductModel3d(productId, modelUrl);
            postSuccess(callback, ok);
            return;
        }

        Map<String, String> body = new HashMap<>();
        body.put("model3dUrl", modelUrl);
        ApiClient.getApi().updateProductModel3d(productId, body).enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    postSuccess(callback, true);
                } else {
                    postError(callback, parseHttpError(response));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                postError(callback, parseNetworkError(t));
            }
        });
    }

    @Override
    public void delete(String id, Callback<Boolean> callback) {
        if (useMock) {
            postSuccess(callback, true);
            return;
        }

        ApiClient.getApi().deleteProduct(id).enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    postSuccess(callback, true);
                } else {
                    postError(callback, parseHttpError(response));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                postError(callback, parseNetworkError(t));
            }
        });
    }

    public static Map<String, String> toQuery(FilterCriteria filter) {
        Map<String, String> map = new HashMap<>();
        if (filter == null) return map;
        if (filter.getCategoryId() != null && !filter.getCategoryId().trim().isEmpty()) {
            map.put("categoryId", filter.getCategoryId().trim());
        }
        if (filter.getMinPrice() != null) {
            map.put("minPrice", String.valueOf(filter.getMinPrice()));
        }
        if (filter.getMaxPrice() != null) {
            map.put("maxPrice", String.valueOf(filter.getMaxPrice()));
        }
        if (filter.isArOnly()) {
            map.put("arOnly", "true");
        }
        if (filter.getSearchQuery() != null && !filter.getSearchQuery().trim().isEmpty()) {
            map.put("searchQuery", filter.getSearchQuery().trim());
        }
        if (filter.getSortBy() != null && !filter.getSortBy().trim().isEmpty()) {
            map.put("sortBy", filter.getSortBy().trim());
        }
        if (filter.getSellerId() != null && !filter.getSellerId().trim().isEmpty()) {
            map.put("sellerId", filter.getSellerId().trim());
        }
        return map;
    }
}
